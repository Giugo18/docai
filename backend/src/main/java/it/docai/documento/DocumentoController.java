package it.docai.documento;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
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
    private final ValidatoreUpload validatore;

    public DocumentoController(DocumentoService service, ValidatoreUpload validatoreUpload) {
        this.service = service;
        this.validatore = validatoreUpload;
    }

    @GetMapping
    public List<DocumentoDto> elenca(@AuthenticationPrincipal Jwt jwt) {
        return service.elenca(jwt.getSubject());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> carica(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal Jwt jwt) {
        return switch (validatore.valida(file)) {
            case EsitoValidazione.Valido() ->
                    ResponseEntity.status(HttpStatus.CREATED).body(service.carica(file, jwt.getSubject()));
            case EsitoValidazione.FileVuoto() -> errore(HttpStatus.BAD_REQUEST, "Il file è vuoto");
            case EsitoValidazione.TipoNonAmmesso(var tipo) ->
                    errore(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo di file non ammesso: " + tipo);
            case EsitoValidazione.TroppoGrande(var dimensione, var massimo) -> errore(HttpStatus.CONTENT_TOO_LARGE,
                    "File di %.1f MB, il massimo è %d MB".formatted(dimensione / 1024.0 / 1024, massimo / 1024 / 1024));
        };
    }

    @GetMapping("/riepilogo")
    public RiepilogoDocumenti riepilogo(@AuthenticationPrincipal Jwt jwt) {
        return service.riepilogo(jwt.getSubject());
    }

    private ResponseEntity<ProblemDetail> errore(HttpStatus stato, String dettaglio) {
        return ResponseEntity.status(stato).body(ProblemDetail.forStatusAndDetail(stato, dettaglio));
    }

    @GetMapping("/{id}")
    public DocumentoDto trova(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.trova(id, jwt.getSubject());
    }
}
