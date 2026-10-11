package it.docai.rag;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversazioni")
public class ConversazioneController {

    private final ConversazioneService service;

    public ConversazioneController(ConversazioneService service) {
        this.service = service;
    }

    /*@GetMapping
    List<ConversazioneDto> elenca(@AuthenticationPrincipal Jwt jwt) {
        return service.elenca(jwt.getSubject());
    }*/

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ConversazioneDto crea(@Valid @RequestBody NuovaConversazione richiesta, @AuthenticationPrincipal Jwt jwt) {
        return service.crea(richiesta, jwt.getSubject());
    }

    @GetMapping("/{id}")
    DettaglioConversazione apri(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.apri(id, jwt.getSubject());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void elimina(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        service.elimina(id, jwt.getSubject());
    }

    @GetMapping
    List<ConversazioneDto> elenca(@RequestParam(required = false) UUID documentoId, @AuthenticationPrincipal Jwt jwt) {
        return service.elenca(jwt.getSubject(), documentoId);
    }
}
