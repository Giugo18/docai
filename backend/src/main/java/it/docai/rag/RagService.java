package it.docai.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    static final int CHUNK_DA_USARE = 5;
    static final double SOGLIA_MINIMA = 0.3;
    static final String NIENTE = "Non trovo questa informazione nei tuoi documenti.";

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final String regole;

    public RagService(VectorStore vectorStore, ChatClient chatClient,
                      @Value("classpath:prompts/rag.st") Resource prompt) throws IOException {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
        this.regole = prompt.getContentAsString(StandardCharsets.UTF_8);
    }

    public RispostaDocumenti chiedi(String domanda, UUID documentoId, String proprietario) {
        // 1. retrieve: solo i chunk di chi chiede
        List<Document> trovati = cerca(domanda, documentoId, proprietario);
        if (trovati.isEmpty()) {
            return new RispostaDocumenti(NIENTE, List.of());       // 2. niente modello
        }

        // 3. augment: fonti numerate nel prompt
        List<Fonte> fonti = new ArrayList<>();
        var contesto = new StringBuilder();
        for (int i = 0; i < trovati.size(); i++) {
            Document d = trovati.get(i);
            var fonte = fonte(i + 1, d);
            fonti.add(fonte);
            contesto.append("[Fonte %d] %s, pagina %d%n%s%n%n"
                    .formatted(fonte.numero(), fonte.nomeFile(), fonte.pagina(), d.getText()));
        }

        // 4. generate
        String risposta = chatClient.prompt()
                .system(regole)                                   // sostituisce il system della chat
                .user("FONTI:\n" + contesto + "\nDOMANDA: " + domanda)
                .options(ChatOptions.builder().temperature(0.2))
                .call()
                .content();

        log.info("RAG: '{}' → {} fonti, migliore {}", domanda, fonti.size(),
                fonti.getFirst().punteggio());
        return new RispostaDocumenti(risposta, fonti);
    }

    private List<Document> cerca(String domanda, UUID documentoId, String proprietario) {
        var b = new FilterExpressionBuilder();
        var filtro = documentoId == null
                ? b.eq("proprietario", proprietario)
                : b.and(b.eq("proprietario", proprietario), b.eq("documentoId", documentoId.toString()));

        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(domanda)
                .topK(CHUNK_DA_USARE)
                .similarityThreshold(SOGLIA_MINIMA)
                .filterExpression(filtro.build())
                .build());
    }

    private static Fonte fonte(int numero, Document d) {
        var m = d.getMetadata();
        String testo = d.getText();
        return new Fonte(
                numero,
                (String) m.get("documentoId"),
                (String) m.get("nomeFile"),
                ((Number) m.get("pagina")).intValue(),
                d.getScore(),
                testo.substring(0, Math.min(200, testo.length())));
    }
}