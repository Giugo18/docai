package it.docai.rag;

import it.docai.documento.DocumentoEliminato;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PuliziaChunk {

    private final Indicizzatore indicizzatore;

    public PuliziaChunk(Indicizzatore indicizzatore) {
        this.indicizzatore = indicizzatore;
    }

    /** Nella stessa transazione della DELETE del documento: o tutto o niente. */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void quandoEliminato(DocumentoEliminato evento) {
        indicizzatore.cancellaChunk(evento.documentoId());
    }
}