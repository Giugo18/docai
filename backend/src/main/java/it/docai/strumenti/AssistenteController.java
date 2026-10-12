package it.docai.strumenti;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistente")
public class AssistenteController {

    public record Domanda(@NotBlank @Size(max = 1000) String domanda) {}
    public record Risposta(String risposta) {}

    private final AssistenteService assistente;

    public AssistenteController(AssistenteService assistente) {
        this.assistente = assistente;
    }

    @PostMapping
    public Risposta chiedi(@Valid @RequestBody Domanda d, @AuthenticationPrincipal Jwt jwt) {
        return new Risposta(assistente.chiedi(d.domanda(), jwt.getSubject()));
    }
}