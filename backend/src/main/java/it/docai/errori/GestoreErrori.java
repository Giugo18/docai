package it.docai.errori;

import it.docai.documento.DocumentoNonTrovatoException;
import it.docai.documento.ErroreUpload;
import it.docai.documento.UploadNonValidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
@RestControllerAdvice
public class GestoreErrori extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GestoreErrori.class);

    @ExceptionHandler(DocumentoNonTrovatoException.class)
    public ProblemDetail documentoNonTrovato(DocumentoNonTrovatoException ex) {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problema.setTitle("Documento non trovato");
        problema.setType(URI.create("https://docai.it/errori/documento-non-trovato"));
        problema.setProperty("documentoId", ex.getId());
        return problema;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail erroreImprevisto(Exception ex) {
        log.error("Errore imprevisto", ex);   // lo stack trace va nel log, non al client
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Si è verificato un errore interno");
    }

    @ExceptionHandler(UploadNonValidoException.class)
    public ProblemDetail uploadNonValido(UploadNonValidoException ex) {
        return switch (ex.getErrore()) {
            case ErroreUpload.FileVuoto() ->
                    ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Il file è vuoto");
            case ErroreUpload.TipoNonAmmesso(var tipo) ->
                    ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                            "Tipo di file non ammesso: " + tipo);
            case ErroreUpload.TroppoGrande(var dimensione, var massimo) ->
                    ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE,
                            "File di %.1f MB, il massimo è %d MB".formatted(dimensione / 1024.0 / 1024, massimo / 1024 / 1024));
        };
    }
}
