package it.docai.chat;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

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
    @PostMapping
    public ProblemDetail chiedi(@RequestBody DomandaRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_IMPLEMENTED,
                "La chat arriva nella settimana 6 del piano");
    }
}
