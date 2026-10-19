package it.docai.errori;

import com.openai.errors.OpenAIException;
import com.openai.errors.RateLimitException;
import it.docai.config.IdRichiestaFilter;
import it.docai.documento.DocumentoNonTrovatoException;
import it.docai.documento.ErroreUpload;
import it.docai.documento.UploadNonValidoException;
import it.docai.estrazione.ContenutoNonDisponibileException;
import it.docai.estrazione.DocumentoTroppoLungoException;
import it.docai.estrazione.TestoNonEstraibileException;
import it.docai.rag.ConversazioneNonTrovataException;
import it.docai.strumenti.ModelloOccupatoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GestoreErrori extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GestoreErrori.class);

    @ExceptionHandler(DocumentoNonTrovatoException.class)
    public ProblemDetail documentoNonTrovato(DocumentoNonTrovatoException ex) {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problema.setTitle("Documento non trovato");
        problema.setType(URI.create("https://docai.it/errori/documento-non-trovato"));
        problema.setProperty("documentoId", ex.getId());
        return conId(problema);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail erroreImprevisto(Exception ex) {
        log.error("Errore imprevisto", ex);   // lo stack trace va nel log, non al client
        return conId(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Si è verificato un errore interno"));
    }

    /**
     * Nello streaming gli errori del client OpenAI (Groq) arrivano avvolti in una CompletionException:
     * Spring guarda prima l'eccezione esterna, che finirebbe in erroreImprevisto. Qui si guarda la causa.
     */
    @ExceptionHandler(CompletionException.class)
    public ProblemDetail completamentoFallito(CompletionException ex) {
        if (ex.getCause() instanceof OpenAIException causa) {
            return servizioAi(causa);
        }
        return erroreImprevisto(ex);
    }

    /** Chiamata sincrona (call()): l'eccezione del client OpenAI arriva senza involucro. */
    @ExceptionHandler(OpenAIException.class)
    public ProblemDetail servizioAiFallito(OpenAIException ex) {
        return servizioAi(ex);
    }

    @ExceptionHandler(UploadNonValidoException.class)
    public ProblemDetail uploadNonValido(UploadNonValidoException ex) {
        return conId(switch (ex.getErrore()) {
            case ErroreUpload.FileVuoto() ->
                    ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Il file è vuoto");
            case ErroreUpload.TipoNonAmmesso(var tipo) ->
                    ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                            "Tipo di file non ammesso: " + tipo);
            case ErroreUpload.TroppoGrande(var dimensione, var massimo) ->
                    ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE,
                            "File di %.1f MB, il massimo è %d MB".formatted(dimensione / 1024.0 / 1024, massimo / 1024 / 1024));
            case ErroreUpload.NonUnPdf() ->
                    ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                            "Il file non è un PDF valido");
        });
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        Map<String, String> errori = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        e -> Objects.requireNonNullElse(e.getDefaultMessage(), "Valore non valido"),
                        (primo, secondo) -> primo));

        ProblemDetail problema = ex.getBody();
        problema.setType(URI.create("https://docai.it/errori/richiesta-non-valida"));
        problema.setTitle("Richiesta non valida");
        problema.setDetail(String.join("; ", errori.values()));
        problema.setProperty("errori", errori);

        return handleExceptionInternal(ex, problema, headers, status, request);   // l'id lo aggiunge createResponseEntity
    }

    /**
     * Tutti gli errori gestiti da ResponseEntityExceptionHandler (405, 415, body illeggibile, validazione…)
     * passano di qui prima di diventare una risposta: un solo punto per aggiungere l'id anche a loro.
     */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
                                                          HttpStatusCode status, WebRequest request) {
        if (body instanceof ProblemDetail problema) {
            conId(problema);
        }
        return super.createResponseEntity(body, headers, status, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail accessoNegato(AccessDeniedException ex) {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "Non hai i permessi per questa operazione");
        problema.setTitle("Accesso negato");
        problema.setType(URI.create("https://docai.it/errori/accesso-negato"));
        return conId(problema);
    }

    @ExceptionHandler(ContenutoNonDisponibileException.class)
    ProblemDetail contenutoNonDisponibile(ContenutoNonDisponibileException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problema.setTitle("Contenuto non disponibile");
        problema.setType(URI.create("https://docai.it/errori/contenuto-non-disponibile"));
        return conId(problema);
    }

    @ExceptionHandler(TestoNonEstraibileException.class)
    ProblemDetail testoNonEstraibile(TestoNonEstraibileException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problema.setTitle("Testo non estraibile");
        problema.setType(URI.create("https://docai.it/errori/testo-non-estraibile"));
        return conId(problema);
    }

    @ExceptionHandler(DocumentoTroppoLungoException.class)
    ProblemDetail documentoTroppoLungo(DocumentoTroppoLungoException e) {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problema.setTitle("Documento troppo lungo");
        problema.setType(URI.create("https://docai.it/errori/documento-troppo-lungo"));
        return conId(problema);
    }

    @ExceptionHandler(ConversazioneNonTrovataException.class)
    ProblemDetail conversazioneNonTrovata(ConversazioneNonTrovataException e) {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problema.setTitle("Conversazione non trovata");
        problema.setType(URI.create("https://docai.it/errori/conversazione-non-trovata"));
        return conId(problema);
    }

    @ExceptionHandler(ModelloOccupatoException.class)
    ProblemDetail modelloOccupato(ModelloOccupatoException e) {
        return assistenteOccupato();
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail modificatoNelFrattempo(OptimisticLockingFailureException e) {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Il documento è stato modificato nel frattempo: ricarica la pagina e riprova");
        return conId(problema);
    }

    /**
     * Groq ha risposto con un errore o non è raggiungibile. Il troppo traffico (429) ha già il suo messaggio;
     * il resto (chiave non valida, servizio giù, rete) per l'utente è "servizio AI non disponibile".
     */
    private ProblemDetail servizioAi(OpenAIException causa) {
        if (causa instanceof RateLimitException) {
            log.warn("Limite di richieste del servizio AI: {}", causa.getMessage());
            return assistenteOccupato();
        }
        log.error("Servizio AI non disponibile: {}", causa.toString());   // il messaggio basta: lo stack è tutto del client HTTP
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Il servizio di intelligenza artificiale non è disponibile in questo momento: riprova tra qualche minuto.");
        problema.setTitle("Servizio AI non disponibile");
        problema.setType(URI.create("https://docai.it/errori/servizio-ai-non-disponibile"));
        return conId(problema);
    }

    private static ProblemDetail assistenteOccupato() {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                "L'assistente ha raggiunto il limite di richieste: riprova tra un minuto, "
                        + "oppure apri una nuova conversazione.");
        pd.setTitle("Assistente occupato");
        return conId(pd);
    }

    /** L'id della richiesta nel corpo dell'errore: chi vede l'errore può citarlo, e lo si trova subito nel log. */
    private static ProblemDetail conId(ProblemDetail problema) {
        problema.setProperty("idRichiesta", MDC.get(IdRichiestaFilter.CHIAVE_MDC));
        return problema;
    }
}