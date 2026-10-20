package it.docai.rag;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "docai.valutazione.attiva", havingValue = "true")
public class ValutazioneController {

    private final Valutazione valutazione;

    public ValutazioneController(Valutazione valutazione) {
        this.valutazione = valutazione;
    }

    /**
     * Senza parametri esegue tutti i casi. Per risparmiare token se ne può eseguire una parte:
     * ?modalita=AGENTE (RAG, ASSISTENTE, AGENTE) e/o ?contiene=java (testo nell'etichetta del caso).
     */
    @PostMapping("/api/rag/valutazione")
    ReportValutazione esegui(@AuthenticationPrincipal Jwt jwt,
                             @RequestParam(required = false) CasoDiProva.Modalita modalita,
                             @RequestParam(required = false) String contiene) {
        return valutazione.esegui(jwt.getSubject(), modalita, contiene);
    }
}