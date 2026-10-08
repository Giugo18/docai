package it.docai.documento;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documenti")
public class DocumentoController {

    private final DocumentoService service;

    public DocumentoController(DocumentoService service) {
        this.service = service;
    }

    @GetMapping
    public List<DocumentoDto> elenca(@AuthenticationPrincipal Jwt jwt) {
        return service.elenca(jwt.getSubject());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentoDto carica(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal Jwt jwt) {
        return service.carica(file, jwt.getSubject());
    }

    @GetMapping("/riepilogo")
    public RiepilogoDocumenti riepilogo(@AuthenticationPrincipal Jwt jwt) {
        return service.riepilogo(jwt.getSubject());
    }


    @GetMapping("/{id}")
    public DocumentoDto trova(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.trova(id, jwt.getSubject());
    }

    @PatchMapping("/{id}")
    public DocumentoDto rinomina(@PathVariable UUID id,
                                 @Valid @RequestBody RinominaDocumento richiesta,
                                 @AuthenticationPrincipal Jwt jwt) {
        return service.rinomina(id, richiesta.nomeFile(), jwt.getSubject());
    }
}
