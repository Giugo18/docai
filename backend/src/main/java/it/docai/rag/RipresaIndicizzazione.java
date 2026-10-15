package it.docai.rag;

import it.docai.config.IdRichiestaFilter;
import it.docai.documento.DocumentoRepository;
import it.docai.documento.StatoDocumento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Ogni tanto riprova i documenti finiti in ERRORE (es. Ollama spento), fino a un massimo di tentativi. */
@Component
public class RipresaIndicizzazione {

    private static final Logger log = LoggerFactory.getLogger(RipresaIndicizzazione.class);

    private final DocumentoRepository repository;
    private final IndicizzazioneAutomatica indicizzazione;
    private final int massimoTentativi;

    public RipresaIndicizzazione(DocumentoRepository repository, IndicizzazioneAutomatica indicizzazione,
                                 @Value("${docai.indicizzazione.massimo-tentativi:3}") int massimoTentativi) {
        this.repository = repository;
        this.indicizzazione = indicizzazione;
        this.massimoTentativi = massimoTentativi;
    }

    @Scheduled(fixedDelayString = "${docai.indicizzazione.ripresa:PT10M}", initialDelayString = "PT2M")
    public void riprovaInErrore() {
        MDC.put(IdRichiestaFilter.CHIAVE_MDC, "batch-" + UUID.randomUUID().toString().substring(0, 4));
        //log.info("Ripresa: controllo dei documenti in errore");
        try {
            var daRiprovare = repository.findByStatoAndTentativiIndicizzazioneLessThan(
                    StatoDocumento.ERRORE, massimoTentativi);
            if (!daRiprovare.isEmpty()) {
                log.info("Ripresa: {} documenti in errore da riprovare", daRiprovare.size());
            }
            daRiprovare.forEach(d -> indicizzazione.accoda(d.getId()));   // passa dal proxy: @Async, nel pool
        } finally {
            MDC.remove(IdRichiestaFilter.CHIAVE_MDC);
        }
    }
}