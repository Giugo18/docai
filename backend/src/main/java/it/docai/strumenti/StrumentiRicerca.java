package it.docai.strumenti;

import it.docai.rag.Fonte;
import it.docai.rag.RicercaIbrida;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class StrumentiRicerca {

    private static final Logger log = LoggerFactory.getLogger(StrumentiRicerca.class);
    private static final int PASSAGGI = 5;
    private static final double SOGLIA = 0.3;

    private final RicercaIbrida ricerca;

    public StrumentiRicerca(RicercaIbrida ricerca) {
        this.ricerca = ricerca;
    }

    /** Un passaggio trovato: il numero serve al modello per citarlo come [Fonte N]. */
    public record Passaggio(int fonte, String nomeFile, int pagina, String testo) {}

    @Tool(description = """
            Cerca nel contenuto dei documenti dell'utente i passaggi più pertinenti \
            (ricerca semantica e per parole). Restituisce fino a 5 passaggi con numero di fonte, \
            nome del file, pagina e testo. Usalo per domande su cosa c'è scritto nei documenti: \
            importi, nomi, date, definizioni, spiegazioni.""")
    @SuppressWarnings("unchecked")
    public List<Passaggio> cercaNeiDocumenti(
            @ToolParam(description = "Cosa cercare: una domanda completa e autonoma, in italiano") String domanda,
            ToolContext contesto) {
        Map<String, Object> c = contesto.getContext();
        String proprietario = (String) c.get(ContestoStrumenti.PROPRIETARIO);
        UUID documentoId = (UUID) c.get(ContestoStrumenti.DOCUMENTO);           // null = tutti i documenti
        List<Fonte> raccolte = (List<Fonte>) c.get(ContestoStrumenti.FONTI);
        log.info("Tool cercaNeiDocumenti '{}' per {}", domanda, proprietario);

        var passaggi = new ArrayList<Passaggio>();
        for (Document d : ricerca.cerca(domanda, documentoId, proprietario, PASSAGGI, SOGLIA)) {
            var m = d.getMetadata();
            int numero = raccolte.size() + 1;                                      // numerazione unica
            String nomeFile = (String) m.get("nomeFile");
            int pagina = ((Number) m.get("pagina")).intValue();
            String testo = d.getText();
            raccolte.add(new Fonte(numero, (String) m.get("documentoId"), nomeFile, pagina,
                    d.getScore(), testo.substring(0, Math.min(200, testo.length()))));
            passaggi.add(new Passaggio(numero, nomeFile, pagina, testo));
        }
        return passaggi;
    }
}