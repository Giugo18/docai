package it.docai.strumenti;

import it.docai.documento.DocumentoDto;
import it.docai.documento.DocumentoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class StrumentiDocumenti {

    private static final Logger log = LoggerFactory.getLogger(StrumentiDocumenti.class);
    static final String PROPRIETARIO = ContestoStrumenti.PROPRIETARIO;

    private final DocumentoService documenti;

    public StrumentiDocumenti(DocumentoService documenti) {
        this.documenti = documenti;
    }

    /** Quello che il modello vede di un documento: niente id interni, niente contenuto. */
    public record DocumentoInBreve(String nomeFile, String stato, Instant caricatoIl) {
        static DocumentoInBreve da(DocumentoDto d) {
            return new DocumentoInBreve(d.nomeFile(), d.stato().toString(), d.caricatoIl());
        }
    }

    @Tool(description = """
            Elenca i documenti caricati dall'utente: nome del file, stato dell'indicizzazione \
            (CARICATO, IN_INDICIZZAZIONE, INDICIZZATO, ERRORE) e data di caricamento. \
            Usalo per domande su quanti o quali documenti ha l'utente o sul loro stato.""")
    public List<DocumentoInBreve> elencaDocumenti(ToolContext contesto) {
        String proprietario = (String) contesto.getContext().get(PROPRIETARIO);
        log.info("Tool elencaDocumenti per {}", proprietario);
        return documenti.elenca(proprietario).stream().map(DocumentoInBreve::da).toList();
    }
}