package it.docai.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class Valutazione {

    private static final Logger log = LoggerFactory.getLogger(Valutazione.class);

    private final RagService rag;
    private final List<CasoDiProva> casi;

    public Valutazione(RagService rag, JsonMapper json,
                       @Value("classpath:valutazione/domande.json") Resource file) throws IOException {
        this.rag = rag;
        this.casi = json.readValue(file.getInputStream(), new TypeReference<List<CasoDiProva>>() {});

        log.info("Valutazione attiva: {} casi di prova caricati", casi.size());
    }

    public ReportValutazione esegui(String proprietario) {
        var esiti = new ArrayList<EsitoCaso>();
        for (CasoDiProva caso : casi) {
            long inizio = System.currentTimeMillis();
            RispostaDocumenti r = rag.chiedi(caso.domanda(), null, proprietario);   // su TUTTI i documenti
            long durata = System.currentTimeMillis() - inizio;

            Integer posizione = null;
            for (Fonte f : r.fonti()) {
                if (f.nomeFile().equals(caso.documento())) {
                    posizione = f.numero();
                    break;
                }
            }


            boolean fonteTrovata = caso.nonDeveTrovare() || posizione != null;
            boolean corretta = caso.nonDeveTrovare()
                    ? normalizza(r.risposta()).contains("non trovo")
                    : contieneTutte(r.risposta(), caso.paroleAttese());

            esiti.add(new EsitoCaso(caso.domanda(), fonteTrovata, posizione, corretta, durata, r.risposta()));
            log.info("Valutazione: {} | fonte={} pos={} risposta={} | {} ms",
                    caso.domanda(), fonteTrovata ? "OK" : "NO", posizione, corretta ? "OK" : "NO", durata);
        }

        int fonti = (int) esiti.stream().filter(EsitoCaso::fonteTrovata).count();
        int corrette = (int) esiti.stream().filter(EsitoCaso::rispostaCorretta).count();
        long medio = (long) esiti.stream().mapToLong(EsitoCaso::millisecondi).average().orElse(0);
        log.info("Valutazione: fonti {}/{}, risposte {}/{}, tempo medio {} ms",
                fonti, esiti.size(), corrette, esiti.size(), medio);
        return new ReportValutazione(esiti.size(), fonti, corrette, medio, esiti);
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
}