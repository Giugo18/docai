package it.docai.documento;

import java.time.Instant;
import java.util.UUID;

/** Record Java 21: DTO immutabile esposto dalle API, separato dall'entità JPA. */
public record DocumentoDto(UUID id, String nomeFile, long dimensione, StatoDocumento stato, Instant caricatoIl) {

    static DocumentoDto da(Documento d) {
        return new DocumentoDto(d.getId(), d.getNomeFile(), d.getDimensione(), d.getStato(), d.getCaricatoIl());
    }
}
