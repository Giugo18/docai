package it.docai.rag;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "docai.valutazione.attiva", havingValue = "true")
public class ValutazioneController {

    private final Valutazione valutazione;

    public ValutazioneController(Valutazione valutazione) {
        this.valutazione = valutazione;
    }

    @PostMapping("/api/rag/valutazione")
    ReportValutazione esegui(@AuthenticationPrincipal Jwt jwt) {
        return valutazione.esegui(jwt.getSubject());
    }
}