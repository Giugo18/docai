package it.docai.strumenti;

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
    public List<Passaggio> cercaNeiDocumenti(
            @ToolParam(description = "Cosa cercare: una domanda completa e autonoma, in italiano") String domanda,
            ToolContext contesto) {
        String proprietario = (String) contesto.getContext().get(StrumentiDocumenti.PROPRIETARIO);
        log.info("Tool cercaNeiDocumenti '{}' per {}", domanda, proprietario);

        List<Document> trovati = ricerca.cerca(domanda, null, proprietario, PASSAGGI, SOGLIA);
        var passaggi = new ArrayList<Passaggio>();
        for (int i = 0; i < trovati.size(); i++) {
            Document d = trovati.get(i);
            var m = d.getMetadata();
            passaggi.add(new Passaggio(i + 1, (String) m.get("nomeFile"),
                    ((Number) m.get("pagina")).intValue(), d.getText()));
        }
        return passaggi;
    }
}