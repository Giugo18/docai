package it.docai.rag;

import it.docai.strumenti.AssistenteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
public class Valutazione {

    private static final Logger log = LoggerFactory.getLogger(Valutazione.class);

    private final RagService rag;
    private final List<CasoDiProva> casi;
    private final Duration pausa;
    private static final String UTENTE_VUOTO = "valutazione-utente-vuoto";   // nessun documento ha questo proprietario
    private final AssistenteService assistente;

    public Valutazione(RagService rag, JsonMapper json,
                       @Value("classpath:valutazione/domande.json") Resource file,
                       @Value("${docai.valutazione.pausa:4s}") Duration pausa,
                       AssistenteService assistente) throws IOException {
        this.rag = rag;
        this.pausa = pausa;
        this.casi = json.readValue(file.getInputStream(), new TypeReference<List<CasoDiProva>>() {});
        this.assistente = assistente;
        log.info("Valutazione attiva: {} casi di prova caricati, pausa {} tra un caso e l'altro", casi.size(), pausa);
    }

    public ReportValutazione esegui(String proprietario) {
        var esiti = new ArrayList<EsitoCaso>();
        for (CasoDiProva caso : casi) {
            if (!esiti.isEmpty() && !aspetta()) {
                break;                                   // thread interrotto: mi fermo con quello che ho
            }
            long inizio = System.currentTimeMillis();
            RispostaDocumenti r;
            try {
                r = rispondi(caso, proprietario);
            } catch (RuntimeException e) {
                long durata = System.currentTimeMillis() - inizio;
                log.warn("Valutazione: {} | ERRORE {} | {} ms", caso.etichetta(), e.getMessage(), durata);
                esiti.add(EsitoCaso.errore(caso.etichetta(), e.getMessage(), durata));
                continue;
            }
            long durata = System.currentTimeMillis() - inizio;

            Set<Integer> numeriDocumento = r.fonti().stream()
                    .filter(f -> f.nomeFile().equals(caso.documento()))
                    .map(Fonte::numero)
                    .collect(Collectors.toSet());
            Integer posizione = numeriDocumento.stream().min(Integer::compare).orElse(null);   // la migliore

            boolean senzaFonte = caso.nonDeveTrovare() || caso.documento() == null;   // niente da trovare
            boolean fonteTrovata = senzaFonte || posizione != null;
            boolean corretta = corretta(caso, r.risposta());

            Set<Integer> citate = Citazioni.numeri(r.risposta());
            Set<Integer> inventate = Citazioni.inventate(r.risposta(), r.fonti().stream().map(Fonte::numero).toList());
            boolean fonteCitata = senzaFonte || citate.stream().anyMatch(numeriDocumento::contains);

            esiti.add(new EsitoCaso(caso.etichetta(), fonteTrovata, posizione, fonteCitata, inventate,
                    corretta, durata, r.risposta()));
            log.info("Valutazione: {} | fonte={} pos={} citata={} inventate={} risposta={} | {} ms",
                    caso.etichetta(), fonteTrovata ? "OK" : "NO", posizione, fonteCitata ? "OK" : "NO",
                    inventate, corretta ? "OK" : "NO", durata);
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

    private static boolean contieneTutte(String testo, List<String> parole) {
        String t = normalizza(testo);
        return parole.stream().allMatch(p -> t.contains(normalizza(p)));
    }

    private static String normalizza(String testo) {
        return testo.toLowerCase(Locale.ITALIAN)
                .replaceAll("[\\u2010-\\u2015\\u2212]", "-")   // trattini tipografici → '-'
                .replaceAll("[\\u2018\\u2019]", "'")            // apostrofi tipografici → '
                .replaceAll("[\\u201C\\u201D]", "\"")           // virgolette tipografiche → "
                .replace('\u00A0', ' ');                        // spazio non separabile → spazio
    }

    private RispostaDocumenti rispondi(CasoDiProva caso, String proprietario) {
        String chi = caso.utenteVuoto() ? UTENTE_VUOTO : proprietario;
        return switch (caso.modalita()) {
            case RAG -> rag.chiedi(caso.domanda(), null, chi);
            case ASSISTENTE -> assistente.chiediConFonti(caso.domanda(), chi);
        };
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
}