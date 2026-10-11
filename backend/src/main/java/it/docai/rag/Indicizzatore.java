package it.docai.rag;

import it.docai.documento.ContenutoDocumentoRepository;
import it.docai.documento.Documento;
import it.docai.estrazione.ContenutoNonDisponibileException;
import it.docai.estrazione.TestoPdf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class Indicizzatore {

    private static final Logger log = LoggerFactory.getLogger(Indicizzatore.class);

    private final ContenutoDocumentoRepository contenuti;
    private final TestoPdf testoPdf;
    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter = TokenTextSplitter.builder()
            .withChunkSize(400)
            .build();
    private final JdbcClient jdbc;

    public Indicizzatore(ContenutoDocumentoRepository contenuti, TestoPdf testoPdf, VectorStore vectorStore,
                         JdbcClient jdbc) {
        this.contenuti = contenuti;
        this.testoPdf = testoPdf;
        this.vectorStore = vectorStore;
        this.jdbc = jdbc;
    }

    /** Niente @Transactional: gli embedding richiedono secondi o minuti. */
    /** Niente @Transactional: gli embedding richiedono secondi o minuti. */
    public EsitoIndicizzazione indicizza(Documento documento) {
        long inizio = System.currentTimeMillis();

        byte[] pdf = contenuti.findById(documento.getId())
                .orElseThrow(() -> new ContenutoNonDisponibileException(documento.getId()))
                .getDati();
        List<String> pagine = testoPdf.perPagina(pdf);

        // 1. una Document per pagina, con i metadati
        var perPagina = new ArrayList<Document>();
        for (int i = 0; i < pagine.size(); i++) {
            String testo = pagine.get(i);
            if (testo.isBlank()) continue;              // pagine vuote o solo immagini
            perPagina.add(new Document(testo, Map.of(
                    "documentoId", documento.getId().toString(),
                    "proprietario", documento.getProprietario(),
                    "nomeFile", documento.getNomeFile(),
                    "pagina", i + 1)));
        }

        // 2. ogni pagina in chunk: i metadati vengono copiati su ogni chunk
        List<Document> chunk = splitter.apply(perPagina);

        // 3. prima i nuovi, poi via i vecchi: il documento non resta mai senza chunk
        List<String> vecchi = idChunk(documento.getId());   // fotografia PRIMA dell'add
        vectorStore.add(chunk);
        if (!vecchi.isEmpty()) {
            vectorStore.delete(vecchi);
        }

        // 4. rete di sicurezza: INDICIZZATO deve voler dire "i chunk ci sono"
        int salvati = idChunk(documento.getId()).size();
        if (salvati != chunk.size()) {
            throw new IllegalStateException("Attesi %d chunk per %s, trovati %d"
                    .formatted(chunk.size(), documento.getNomeFile(), salvati));
        }

        long durata = System.currentTimeMillis() - inizio;
        log.info("Indicizzato {}: {} pagine, {} chunk in {} ms",
                documento.getNomeFile(), pagine.size(), chunk.size(), durata);
        return new EsitoIndicizzazione(pagine.size(), chunk.size(), durata);
    }

    public void cancellaChunk(UUID documentoId) {
        var filtro = new FilterExpressionBuilder()
                .eq("documentoId", documentoId.toString())
                .build();
        vectorStore.delete(filtro);
    }

    private List<String> idChunk(UUID documentoId) {
        return jdbc.sql("SELECT id::text FROM vector_store WHERE metadata->>'documentoId' = :id")
                .param("id", documentoId.toString())
                .query(String.class)
                .list();
    }
}