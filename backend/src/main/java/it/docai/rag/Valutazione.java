package it.docai.rag;

import it.docai.agente.AgenteService;
import it.docai.agente.RispostaAgente;
import it.docai.strumenti.AssistenteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "docai.valutazione.attiva", havingValue = "true")   // in produzione il bean non esiste
public class Valutazione {

    private static final Logger log = LoggerFactory.getLogger(Valutazione.class);
    private static final String UTENTE_VUOTO = "valutazione-utente-vuoto";   // nessun documento ha questo proprietario

    private final RagService rag;
    private final AssistenteService assistente;
    private final AgenteService agente;
    private final List<CasoDiProva> casi;
    private final Duration pausa;

    public Valutazione(RagService rag, AssistenteService assistente, AgenteService agente, JsonMapper json,
                       @Value("classpath:valutazione/domande.json") Resource file,
                       @Value("${docai.valutazione.pausa:4s}") Duration pausa) throws IOException {
        this.rag = rag;
        this.assistente = assistente;
        this.agente = agente;
        this.pausa = pausa;
        this.casi = json.readValue(file.getInputStream(), new TypeReference<List<CasoDiProva>>() {});
        int esecuzioni = casi.stream().mapToInt(CasoDiProva::ripeti).sum();
        log.info("Valutazione attiva: {} casi di prova caricati ({} esecuzioni), pausa {} tra un caso e l'altro",
                casi.size(), esecuzioni, pausa);
    }

    /** Quello che serve alla valutazione, qualunque sia il percorso: un adattatore tra i tre tipi di risposta. */
    private record Esecuzione(String risposta, List<Fonte> fonti, Integer passi, boolean limiteRaggiunto) {
        static Esecuzione di(RispostaDocumenti r) {
            return new Esecuzione(r.risposta(), r.fonti(), null, false);
        }
        static Esecuzione di(RispostaAgente a) {
            return new Esecuzione(a.risposta(), a.fonti(), a.passi(), a.limiteRaggiunto());
        }
    }

    /** Tutti i casi. */
    public ReportValutazione esegui(String proprietario) {
        return esegui(proprietario, null, null);
    }

    /**
     * Solo i casi scelti: modalita null = tutte; contiene null o vuoto = nessun filtro sul testo
     * (cerca nell'etichetta, che comprende modalità, utente, domande precedenti e domanda).
     */
    public ReportValutazione esegui(String proprietario, CasoDiProva.Modalita modalita, String contiene) {
        String testo = contiene == null ? "" : contiene.strip().toLowerCase(Locale.ITALIAN);
        List<CasoDiProva> scelti = casi.stream()
                .filter(c -> modalita == null || c.modalita() == modalita)
                .filter(c -> testo.isEmpty() || c.etichetta().toLowerCase(Locale.ITALIAN).contains(testo))
                .toList();
        log.info("Valutazione: {} casi su {} (modalita={}, contiene='{}')", scelti.size(), casi.size(), modalita, testo);

        var esiti = new ArrayList<EsitoCaso>();
        tutti:
        for (CasoDiProva caso : scelti) {
            for (int giro = 1; giro <= caso.ripeti(); giro++) {
                if (!esiti.isEmpty() && !aspetta()) {
                    break tutti;                         // thread interrotto: mi fermo con quello che ho
                }
                String etichetta = caso.ripeti() > 1 ? caso.etichetta() + " #" + giro : caso.etichetta();
                esiti.add(valuta(caso, etichetta, proprietario));
            }
        }

        int fonti = conta(esiti, EsitoCaso::fonteTrovata);
        int citate = conta(esiti, EsitoCaso::fonteCitata);
        int valide = conta(esiti, EsitoCaso::citazioniValide);
        int corrette = conta(esiti, EsitoCaso::rispostaCorretta);
        long medio = (long) esiti.stream().mapToLong(EsitoCaso::millisecondi).average().orElse(0);
        log.info("Valutazione: fonti trovate {}/{}, citate {}/{}, citazioni valide {}/{}, risposte {}/{}, tempo medio {} ms",
                fonti, esiti.size(), citate, esiti.size(), valide, esiti.size(), corrette, esiti.size(), medio);
        return new ReportValutazione(esiti.size(), fonti, citate, valide, corrette, medio, esiti);
    }

    /** Una esecuzione di un caso: risposta, controlli, riga di log. */
    private EsitoCaso valuta(CasoDiProva caso, String etichetta, String proprietario) {
        long inizio = System.currentTimeMillis();
        Esecuzione r;
        try {
            r = rispondi(caso, proprietario);
        } catch (RuntimeException e) {
            long durata = System.currentTimeMillis() - inizio;
            log.warn("Valutazione: {} | ERRORE {} | {} ms", etichetta, e.getMessage(), durata);
            return EsitoCaso.errore(etichetta, e.getMessage(), durata);
        }
        long durata = System.currentTimeMillis() - inizio;

        Set<Integer> numeriDocumento = r.fonti().stream()
                .filter(f -> f.nomeFile().equals(caso.documento()))
                .map(Fonte::numero)
                .collect(Collectors.toSet());
        Integer posizione = numeriDocumento.stream().min(Integer::compare).orElse(null);   // la migliore

        boolean senzaFonte = caso.nonDeveTrovare() || caso.documento() == null;   // niente da trovare
        boolean fonteTrovata = senzaFonte || posizione != null;

        boolean entroIPassi = caso.maxPassi() == null
                || (r.passi() != null && r.passi() <= caso.maxPassi() && !r.limiteRaggiunto());
        boolean corretta = corretta(caso, r.risposta()) && entroIPassi;

        Set<Integer> citate = Citazioni.numeri(r.risposta());
        Set<Integer> inventate = Citazioni.inventate(r.risposta(), r.fonti().stream().map(Fonte::numero).toList());
        boolean fonteCitata = senzaFonte || citate.stream().anyMatch(numeriDocumento::contains);

        log.info("Valutazione: {} | fonte={} pos={} citata={} inventate={} passi={} risposta={} | {} ms",
                etichetta, fonteTrovata ? "OK" : "NO", posizione, fonteCitata ? "OK" : "NO",
                inventate, r.passi(), corretta ? "OK" : "NO", durata);
        return new EsitoCaso(etichetta, fonteTrovata, posizione, fonteCitata, inventate,
                corretta, durata, r.passi(), r.risposta());
    }

    private Esecuzione rispondi(CasoDiProva caso, String proprietario) {
        String chi = caso.utenteVuoto() ? UTENTE_VUOTO : proprietario;
        return switch (caso.modalita()) {
            case RAG -> Esecuzione.di(rag.chiedi(caso.domanda(), null, chi));
            case ASSISTENTE -> Esecuzione.di(assistente.chiediConFonti(caso.domanda(), chi, storia(caso, chi)));
            case AGENTE -> Esecuzione.di(agente.esegui(caso.domanda(), chi));
        };
    }

    /**
     * La conversazione prima della domanda: le domande in "prima" vengono fatte davvero, una dopo l'altra,
     * e la storia si costruisce con le risposte vere del modello, come succede nell'app.
     */
    private List<Message> storia(CasoDiProva caso, String chi) {
        var storia = new ArrayList<Message>();
        for (String precedente : caso.prima()) {
            var risposta = assistente.chiediConFonti(precedente, chi, List.copyOf(storia));
            storia.add(new UserMessage(precedente));
            storia.add(new AssistantMessage(risposta.risposta()));
            aspetta();                                   // anche queste chiamate contano per il limite di Groq
        }
        return storia;
    }

    /** Tutte le paroleAttese, almeno una tra almenoUna, nessuna tra nonDeveContenere. */
    private static boolean corretta(CasoDiProva caso, String risposta) {
        String t = normalizza(risposta);
        boolean ok = caso.nonDeveTrovare()
                ? t.contains("non trovo")
                : caso.paroleAttese().stream().allMatch(p -> t.contains(normalizza(p)))
                && (caso.almenoUna().isEmpty() || caso.almenoUna().stream().anyMatch(p -> t.contains(normalizza(p))));
        return ok && caso.nonDeveContenere().stream().noneMatch(p -> t.contains(normalizza(p)));
    }

    /** Pausa tra i casi, per non superare il limite di token al minuto di Groq. */
    private boolean aspetta() {
        try {
            Thread.sleep(pausa);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();          // si rimette il flag: chi sta sopra deve saperlo
            return false;
        }
    }

    private static int conta(List<EsitoCaso> esiti, Predicate<EsitoCaso> condizione) {
        return (int) esiti.stream().filter(condizione).count();
    }

    private static String normalizza(String testo) {
        return testo.toLowerCase(Locale.ITALIAN)
                .replaceAll("[\\u2010-\\u2015\\u2212]", "-")   // trattini tipografici → '-'
                .replaceAll("[\\u2018\\u2019]", "'")            // apostrofi tipografici → '
                .replaceAll("[\\u201C\\u201D]", "\"")           // virgolette tipografiche → "
                .replace('\u00A0', ' ');                        // spazio non separabile → spazio
    }
}