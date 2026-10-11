package it.docai.rag;

import it.docai.documento.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

@Component
public class IndicizzazioneAutomatica {

    private static final Logger log = LoggerFactory.getLogger(IndicizzazioneAutomatica.class);

    private final DocumentoRepository repository;
    private final DocumentoService documenti;
    private final Indicizzatore indicizzatore;

    public IndicizzazioneAutomatica(DocumentoRepository repository, DocumentoService documenti,
                                    Indicizzatore indicizzatore) {
        this.repository = repository;
        this.documenti = documenti;
        this.indicizzatore = indicizzatore;
    }

    @Async("indicizzazioneExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void quandoCaricato(DocumentoCaricato evento) {
        indicizza(evento);
    }

    /** Al riavvio: riprende i documenti rimasti a metà. */
    /** All'avvio: indicizza i documenti mai indicizzati o rimasti a metà. */
    @Async("indicizzazioneExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void riprendiInterrotti() {
        var daFare = repository.findByStatoIn(
                List.of(StatoDocumento.IN_INDICIZZAZIONE, StatoDocumento.CARICATO));
        log.info("All'avvio: {} documenti da indicizzare", daFare.size());
        for (Documento d : daFare) {
            indicizza(new DocumentoCaricato(d.getId()));
        }
    }

    private void indicizza(DocumentoCaricato evento) {
        var id = evento.documentoId();
        var documento = repository.findById(id).orElse(null);
        if (documento == null) return;                       // cancellato nel frattempo

        documenti.iniziaIndicizzazione(id);
        try {
            indicizzatore.indicizza(documento);
            if (!repository.existsById(id)) {
                // eliminato mentre lo indicizzavamo: via i chunk appena salvati
                indicizzatore.cancellaChunk(id);
                log.info("{} eliminato durante l'indicizzazione: chunk rimossi", documento.getNomeFile());
                return;
            }
            documenti.segnaIndicizzato(id);
        } catch (Exception e) {
            log.error("Indicizzazione fallita per {}", documento.getNomeFile(), e);
            documenti.segnaErrore(id);
        }
    }

    /** Ri-indicizzazione su richiesta: stessa coda, stessi stati dell'upload. */
    @Async("indicizzazioneExecutor")
    public void accoda(UUID documentoId) {
        indicizza(new DocumentoCaricato(documentoId));
    }
}