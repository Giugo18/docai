package it.docai.rag;

import it.docai.documento.Documento;
import it.docai.documento.DocumentoService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final DocumentoService documenti;
    private final IndicizzazioneAutomatica indicizzazioneAutomatica;

    public RagController(DocumentoService documenti,
                         IndicizzazioneAutomatica indicizzazioneAutomatica) {
        this.documenti = documenti;
        this.indicizzazioneAutomatica = indicizzazioneAutomatica;
    }

    @PostMapping("/documenti/{id}/indicizza")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void indicizza(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        Documento documento = documenti.trovaDelProprietario(id, jwt.getSubject());
        indicizzazioneAutomatica.accoda(documento.getId());
    }
}