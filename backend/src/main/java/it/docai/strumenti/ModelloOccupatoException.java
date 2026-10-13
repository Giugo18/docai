package it.docai.strumenti;

import com.openai.errors.OpenAIServiceException;

/** Groq ha rifiutato la richiesta per i limiti del piano: troppo grande (413) o troppe richieste (429). */
public class ModelloOccupatoException extends RuntimeException {

    public ModelloOccupatoException(Throwable causa) {
        super("Il modello AI ha raggiunto il limite di richieste", causa);
    }

    /** Risale la catena delle cause: se trova un 413 o un 429 lo traduce, altrimenti lascia l'errore com'è. */
    public static Throwable traduci(Throwable e) {
        for (Throwable c = e; c != null; c = c.getCause()) {
            if (c instanceof OpenAIServiceException o && (o.statusCode() == 413 || o.statusCode() == 429)) {
                return new ModelloOccupatoException(o);
            }
        }
        return e;
    }
}