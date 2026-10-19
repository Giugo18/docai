package it.docai.strumenti;

import it.docai.documento.Documento;
import it.docai.documento.DocumentoRepository;
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
import java.util.stream.Collectors;

@Component
public class StrumentiRicerca {

    private static final Logger log = LoggerFactory.getLogger(StrumentiRicerca.class);
    private static final int PASSAGGI = 4;
    private static final double SOGLIA = 0.3;
    private static final int CARATTERI_PER_PASSAGGIO = 1_200;
    private final RicercaIbrida ricerca;
    private final DocumentoRepository documentoRepository;

    public StrumentiRicerca(RicercaIbrida ricerca, DocumentoRepository documentoRepository) {
        this.ricerca = ricerca;
        this.documentoRepository = documentoRepository;
    }

    /** Un passaggio trovato: il numero serve al modello per citarlo come [Fonte N]. */
    public record Passaggio(int fonte, String nomeFile, int pagina, String testo) {}

    @Tool(description = """
            Cerca nel contenuto dei documenti dell'utente i passaggi più pertinenti \
            (ricerca semantica e per parole). Restituisce fino a 4 passaggi con numero di fonte, \
            nome del file, pagina e testo. Usalo per domande su cosa c'è scritto nei documenti: \
            importi, nomi, date, definizioni, spiegazioni. \
            Se i passaggi non bastano, puoi richiamarlo con parole diverse.""")
    @SuppressWarnings("unchecked")
    public List<Passaggio> cercaNeiDocumenti(
            @ToolParam(description = """
                    Cosa cercare, in italiano, con le parole della domanda ATTUALE dell'utente. \
                    Aggiungi elementi della conversazione precedente SOLO se la domanda vi rimanda \
                    esplicitamente (es. "e quanto costa?", "quello", "lui"). \
                    Se la domanda ha già un suo argomento (es. "usa java?"), cerca solo quello.""") String domanda,
            ToolContext contesto) {
        Map<String, Object> c = contesto.getContext();
        String proprietario = (String) c.get(ContestoStrumenti.PROPRIETARIO);
        UUID documentoId = (UUID) c.get(ContestoStrumenti.DOCUMENTO);           // null = tutti i documenti
        List<Fonte> raccolte = (List<Fonte>) c.get(ContestoStrumenti.FONTI);
        log.info("Tool cercaNeiDocumenti '{}' per {}", domanda, proprietario);

        List<Document> trovati = cerca(domanda, documentoId, proprietario);
        Map<String, String> nomi = nomiAttuali(trovati);
        var passaggi = new ArrayList<Passaggio>();
        for (Document d : trovati) {
            var m = d.getMetadata();
            int numero = raccolte.size() + 1;                                      // numerazione unica
            String id = (String) m.get("documentoId");
            String nome = nomi.getOrDefault(id, (String) m.get("nomeFile"));      // nome attuale, metadati come riserva
            int pagina = ((Number) m.get("pagina")).intValue();
            String testo = d.getText();
            raccolte.add(new Fonte(numero, id, nome, pagina, d.getScore(),
                    testo.substring(0, Math.min(200, testo.length()))));
            passaggi.add(new Passaggio(numero, nome, pagina, tronca(testo, CARATTERI_PER_PASSAGGIO)));
        }
        return passaggi;
    }

    /** Ricerca condivisa: la usano l'assistente (qui sopra) e il server MCP. */
    public List<Document> cerca(String domanda, UUID documentoId, String proprietario) {
        return ricerca.cerca(domanda, documentoId, proprietario, PASSAGGI, SOGLIA);
    }

    /**
     * Il nome nei metadati del chunk è quello del momento dell'indicizzazione: dopo una rinomina è vecchio.
     * Qui si leggono i nomi attuali dalla tabella documento, con una sola query per tutti i chunk trovati.
     */
    public Map<String, String> nomiAttuali(List<Document> trovati) {
        List<UUID> ids = trovati.stream()
                .map(d -> (String) d.getMetadata().get("documentoId"))
                .distinct()
                .map(UUID::fromString)
                .toList();
        return documentoRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(doc -> doc.getId().toString(), Documento::getNomeFile));
    }

    /** Il passaggio per il modello: testo troncato per restare nel budget di token (usato dal server MCP). */
    public static Passaggio passaggio(int numero, Document d) {
        var m = d.getMetadata();
        return new Passaggio(numero, (String) m.get("nomeFile"),
                ((Number) m.get("pagina")).intValue(), tronca(d.getText(), CARATTERI_PER_PASSAGGIO));
    }

    static String tronca(String testo, int massimo) {
        return testo.length() <= massimo ? testo : testo.substring(0, massimo) + "…";
    }
}