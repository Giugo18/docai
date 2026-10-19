package it.docai.rag;

import it.docai.documento.Documento;
import it.docai.documento.DocumentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RAG "classico" della pagina Chiedi: si cerca prima, poi le fonti trovate vanno al modello insieme alla domanda.
 * Le conversazioni invece usano l'agente con gli strumenti (AssistenteService).
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    static final int CHUNK_DA_USARE = 5;
    static final double SOGLIA_MINIMA = 0.3;
    public static final String NIENTE = "Non trovo questa informazione nei tuoi documenti.";

    private final ChatClient chatClient;
    private final RicercaIbrida ricercaIbrida;
    private final DocumentoRepository documentoRepository;
    private final String regole;

    private record Contesto(List<Fonte> fonti, String messaggio) {}

    public RagService(ChatClient chatClient, RicercaIbrida ricercaIbrida, DocumentoRepository documentoRepository,
                      @Value("classpath:prompts/rag.st") Resource prompt) throws IOException {
        this.chatClient = chatClient;
        this.ricercaIbrida = ricercaIbrida;
        this.documentoRepository = documentoRepository;
        this.regole = prompt.getContentAsString(StandardCharsets.UTF_8);
    }

    public RispostaDocumenti chiedi(String domanda, UUID documentoId, String proprietario) {
        return prepara(domanda, documentoId, proprietario)
                .map(c -> {
                    String testo = Citazioni.normalizza(richiesta(c).call().content());
                    return new RispostaDocumenti(testo, NIENTE.equals(testo.strip()) ? List.of() : c.fonti());
                })
                .orElse(new RispostaDocumenti(NIENTE, List.of()));
    }

    public Flux<EventoRag> chiediInStreaming(String domanda, UUID documentoId, String proprietario) {
        return prepara(domanda, documentoId, proprietario)
                .map(c -> Flux.concat(
                        Flux.just(EventoRag.fonti(c.fonti())),                          // prima le fonti
                        richiesta(c).stream().content().map(EventoRag::testo)))         // poi il testo, a pezzi
                .orElse(Flux.just(EventoRag.fonti(List.of()), EventoRag.testo(NIENTE)));
    }

    private Optional<Contesto> prepara(String domanda, UUID documentoId, String proprietario) {
        List<Document> trovati = ricercaIbrida.cerca(domanda, documentoId, proprietario, CHUNK_DA_USARE, SOGLIA_MINIMA);
        if (trovati.isEmpty()) {
            log.info("RAG: '{}' → nessuna fonte", domanda);
            return Optional.empty();
        }
        Map<String, String> nomi = nomiAttuali(trovati);
        List<Fonte> fonti = new ArrayList<>();
        var contesto = new StringBuilder();
        for (int i = 0; i < trovati.size(); i++) {
            Document d = trovati.get(i);
            var fonte = fonte(i + 1, d, nomi);
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

    /**
     * Il nome nei metadati del chunk è quello del momento dell'indicizzazione: dopo una rinomina è vecchio.
     * Qui si leggono i nomi attuali dalla tabella documento, con una sola query per tutti i chunk trovati.
     */
    private Map<String, String> nomiAttuali(List<Document> trovati) {
        List<UUID> ids = trovati.stream()
                .map(d -> (String) d.getMetadata().get("documentoId"))
                .distinct()
                .map(UUID::fromString)
                .toList();
        return documentoRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(doc -> doc.getId().toString(), Documento::getNomeFile));
    }

    private static Fonte fonte(int numero, Document d, Map<String, String> nomi) {
        var m = d.getMetadata();
        String testo = d.getText();
        String id = (String) m.get("documentoId");
        return new Fonte(
                numero,
                id,
                nomi.getOrDefault(id, (String) m.get("nomeFile")),   // nome attuale; quello dei metadati solo come riserva
                ((Number) m.get("pagina")).intValue(),
                d.getScore(),
                testo.substring(0, Math.min(200, testo.length())));
    }
}