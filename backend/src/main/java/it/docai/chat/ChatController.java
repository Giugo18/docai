package it.docai.chat;

import jakarta.validation.Valid;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * Chat libera con il modello, senza documenti (pagina Chat). Le domande sui documenti passano da
 * RagController: RagService per la pagina Chiedi, AssistenteService per le conversazioni.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping
    public RispostaChat chiedi(@Valid @RequestBody DomandaChat richiesta) {
        String testo = chatClient.prompt()
                .user(richiesta.domanda())   // il messaggio dell'utente
                .call()                      // chiamata sincrona: aspetta la risposta completa
                .content();                  // solo il testo
        return new RispostaChat(testo);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<FrammentoChat> chiediInStreaming(@Valid @RequestBody DomandaChat richiesta) {
        return chatClient.prompt()
                .user(richiesta.domanda())
                .stream()                    // invece di call(): non aspetta la fine
                .content()                   // Flux<String>: i pezzi di testo man mano
                .map(FrammentoChat::new);    // ogni pezzo diventa {"testo": "..."}
    }
}