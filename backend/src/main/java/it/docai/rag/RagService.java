package it.docai.rag;

import org.springframework.ai.chat.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
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
    private final String regoleRiscrittura;
    private final ConversazioneService conversazioneService;



    public RagService(VectorStore vectorStore, ChatClient chatClient, RicercaIbrida ricercaIbrida,
                      @Value("classpath:prompts/rag.st") Resource prompt,
                      @Value("classpath:prompts/riscrittura.st") Resource promptRiscrittura,
                      ConversazioneService conversazioneService
    ) throws IOException {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
        this.ricercaIbrida = ricercaIbrida;
        this.regole = prompt.getContentAsString(StandardCharsets.UTF_8);
        this.regoleRiscrittura = promptRiscrittura.getContentAsString(StandardCharsets.UTF_8);
        this.conversazioneService = conversazioneService;
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
                .map(c -> new RispostaDocumenti(Citazioni.normalizza(richiesta(c).call().content()), c.fonti()))
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
        return ricercaIbrida.cerca(domanda, documentoId, proprietario, CHUNK_DA_USARE, SOGLIA_MINIMA);
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

    public Flux<EventoRag> chiediInConversazione(UUID conversazioneId, String domanda, String proprietario) {
        Conversazione c = conversazioneService.trova(conversazioneId, proprietario);         // 404 se non è mia
        List<Messaggio> storia = conversazioneService.ultimi(c.getId());                     // PRIMA di salvare la nuova
        String autonoma = storia.isEmpty() ? domanda : riscrivi(storia, domanda);
        conversazioneService.registraDomanda(c.getId(), domanda);

        Optional<Contesto> contesto = prepara(autonoma, c.getDocumentoId(), proprietario);
        if (contesto.isEmpty()) {
            conversazioneService.registraRisposta(c.getId(), NIENTE, List.of());
            return Flux.just(EventoRag.riscritta(autonoma), EventoRag.fonti(List.of()), EventoRag.testo(NIENTE), null);
        }

        Contesto ctx = contesto.get();
        var risposta = new StringBuilder();
        Flux<EventoRag> testo = chatClient.prompt()
                .system(regole)
                //.messages(cronologia(storia))                      // la conversazione finora
                .user(ctx.messaggio())                             // fonti + domanda riscritta
                .options(ChatOptions.builder().temperature(0.2))
                .stream()
                .content()
                .doOnNext(risposta::append)                        // accumula i pezzi...
                .doOnComplete(() ->                                // ...e a fine stream salva
                        conversazioneService.registraRisposta(c.getId(), Citazioni.normalizza(risposta.toString()), ctx.fonti()))
                .map(EventoRag::testo);

        return Flux.concat(Flux.just(EventoRag.riscritta(autonoma), EventoRag.fonti(ctx.fonti())), testo, null);
    }

    private String riscrivi(List<Messaggio> storia, String domanda) {
        var testo = new StringBuilder("CONVERSAZIONE:\n");
        for (Messaggio m : storia) {
            testo.append(m.getRuolo() == RuoloMessaggio.UTENTE ? "Utente: " : "Assistente: ")
                    .append(m.getTesto()).append('\n');
        }
        testo.append("\nULTIMA DOMANDA: ").append(domanda);

        String riscritta = chatClient.prompt()
                .system(regoleRiscrittura)
                .user(testo.toString())
                .options(ChatOptions.builder().temperature(0.0))
                .call()
                .content()
                .strip();
        log.info("Riscrittura: '{}' → '{}'", domanda, riscritta);
        return riscritta.isBlank() ? domanda : riscritta;
    }

    private static List<Message> cronologia(List<Messaggio> storia) {
        return storia.stream()
                .<Message>map(m -> m.getRuolo() == RuoloMessaggio.UTENTE
                        ? new UserMessage(m.getTesto())
                        : new AssistantMessage(m.getTesto()))
                .toList();
    }
}