package it.docai.chat;

import jakarta.validation.Valid;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatClient chatClient;

    // Spring Boot crea il Builder già configurato per Ollama (base-url e modello da application.yml)
    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }


    @PostMapping
    public RispostaChat chiedi(@Valid @RequestBody DomandaChat richiesta) {
        String testo = chatClient.prompt()
                .user(richiesta.domanda())   // il messaggio dell'utente
                .call()                      // chiamata sincrona: aspetta la risposta completa
                .content();                  // solo il testo
        return new RispostaChat(testo);
    }


    public record DomandaRequest(String domanda) {}

    /*
     * TODO settimana 6: inietta ChatClient.Builder e costruisci il ChatClient
     *      con un prompt di sistema (es. "Rispondi in italiano, in modo conciso").
     *
     * TODO settimana 9: aggiungi il QuestionAnswerAdvisor sul VectorStore,
     *      con un filtro sui metadati: proprietario == jwt.getSubject()
     *      così ogni utente interroga solo i propri documenti.
     *
     * TODO settimana 10: registra dei tool (@Tool) che interrogano il DB,
     *      es. "quanti documenti ho caricato questo mese?"
     */

}
