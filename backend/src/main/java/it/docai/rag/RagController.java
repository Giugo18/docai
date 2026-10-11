package it.docai.rag;

import it.docai.documento.Documento;
import it.docai.documento.DocumentoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final Indicizzatore indicizzatore;
    private final DocumentoService documenti;

    public RagController(Indicizzatore indicizzatore, DocumentoService documenti) {
        this.indicizzatore = indicizzatore;
        this.documenti = documenti;
    }

    @PostMapping("/documenti/{id}/indicizza")
    EsitoIndicizzazione indicizza(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        Documento documento = documenti.trovaDelProprietario(id, jwt.getSubject());
        return indicizzatore.indicizza(documento);
    }
}