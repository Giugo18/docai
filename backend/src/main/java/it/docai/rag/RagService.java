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
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private record Contesto(List<Fonte> fonti, String messaggio) {}
    private final RicercaIbrida ricercaIbrida;

    public RagService(VectorStore vectorStore, ChatClient chatClient, RicercaIbrida ricercaIbrida,
                      @Value("classpath:prompts/rag.st") Resource prompt) throws IOException {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
        this.ricercaIbrida = ricercaIbrida;
        this.regole = prompt.getContentAsString(StandardCharsets.UTF_8);
    }

    private Optional<Contesto> prepara(String domanda, UUID documentoId, String proprietario) {
        List<Document> trovati = cerca(domanda, documentoId, proprietario);
        if (trovati.isEmpty()) {
            log.info("RAG: '{}' → nessuna fonte", domanda);
            return Optional.empty();
        }
        List<Fonte> fonti = new ArrayList<>();
        var contesto = new StringBuilder();
        for (int i = 0; i < trovati.size(); i++) {
            Document d = trovati.get(i);
            var fonte = fonte(i + 1, d);
            fonti.add(fonte);
            contesto.append("[Fonte %d] %s, pagina %d%n%s%n%n"
                    .formatted(fonte.numero(), fonte.nomeFile(), fonte.pagina(), d.getText()));
        }
        log.info("RAG: '{}' → {} fonti, migliore {}", domanda, fonti.size(), fonti.getFirst().punteggio());
        return Optional.of(new Contesto(fonti, "FONTI:\n" + contesto + "\nDOMANDA: " + domanda));
    }

    private ChatClient.ChatClientRequestSpec richiesta(Contesto c) {
        return chatClient.prompt()
                .system(regole)
                .user(c.messaggio())
                .options(ChatOptions.builder().temperature(0.2));
    }

    public RispostaDocumenti chiedi(String domanda, UUID documentoId, String proprietario) {
        return prepara(domanda, documentoId, proprietario)
                .map(c -> new RispostaDocumenti(richiesta(c).call().content(), c.fonti()))
                .orElse(new RispostaDocumenti(NIENTE, List.of()));
    }

    public Flux<EventoRag> chiediInStreaming(String domanda, UUID documentoId, String proprietario) {
        return prepara(domanda, documentoId, proprietario)
                .map(c -> Flux.concat(
                        Flux.just(EventoRag.fonti(c.fonti())),                          // prima le fonti
                        richiesta(c).stream().content().map(EventoRag::testo)))         // poi il testo, a pezzi
                .orElse(Flux.just(EventoRag.fonti(List.of()), EventoRag.testo(NIENTE)));
    }

    private List<Document> cerca(String domanda, UUID documentoId, String proprietario) {
        List<Document> trovati = ricercaIbrida.cerca(domanda, documentoId, proprietario, CHUNK_DA_USARE, SOGLIA_MINIMA);
        trovati.forEach(d -> log.debug("  {} p.{} sim={} rrf={} parole={}",
                d.getMetadata().get("nomeFile"), d.getMetadata().get("pagina"),
                d.getScore(), d.getMetadata().get("rrf"), d.getMetadata().get("perParole")));
        return trovati;
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