package it.docai.documento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "contenuto_documento")
public class ContenutoDocumento {

    @Id
    @Column(name = "documento_id")
    private UUID documentoId;

    @Column(name = "dati", nullable = false)
    private byte[] dati;            // byte[] → bytea (niente @Lob: in Postgres creerebbe un "large object")

    protected ContenutoDocumento() {
        // per JPA
    }

    public ContenutoDocumento(UUID documentoId, byte[] dati) {
        this.documentoId = documentoId;
        this.dati = dati;
    }

    public UUID getDocumentoId() {
        return documentoId;
    }

    public byte[] getDati() {
        return dati;
    }
}