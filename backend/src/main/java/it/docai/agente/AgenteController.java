package it.docai.agente;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agente")
class AgenteController {

    record Compito(@NotBlank @Size(max = 1000) String compito) {}

    private final AgenteService agente;

    AgenteController(AgenteService agente) {
        this.agente = agente;
    }

    @PostMapping
    RispostaAgente esegui(@Valid @RequestBody Compito richiesta, @AuthenticationPrincipal Jwt jwt) {
        return agente.esegui(richiesta.compito(), jwt.getSubject());
    }
}