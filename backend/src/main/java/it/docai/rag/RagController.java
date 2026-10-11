package it.docai.rag;

import it.docai.documento.Documento;
import it.docai.documento.DocumentoService;
import jakarta.validation.Valid;
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
    private final RagService rag;

    public RagController(DocumentoService documenti,
                         IndicizzazioneAutomatica indicizzazioneAutomatica,
                         RagService rag) {
        this.documenti = documenti;
        this.indicizzazioneAutomatica = indicizzazioneAutomatica;
        this.rag = rag;
    }

    @PostMapping("/documenti/{id}/indicizza")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void indicizza(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        Documento documento = documenti.trovaDelProprietario(id, jwt.getSubject());
        indicizzazioneAutomatica.accoda(documento.getId());
    }

    @PostMapping("/domanda")
    RispostaDocumenti domanda(@Valid @RequestBody DomandaDocumenti richiesta,
                              @AuthenticationPrincipal Jwt jwt) {
        if (richiesta.documentoId() != null) {
            documenti.trovaDelProprietario(richiesta.documentoId(), jwt.getSubject());   // 404 se non è mio
        }
        return rag.chiedi(richiesta.domanda(), richiesta.documentoId(), jwt.getSubject());
    }
}