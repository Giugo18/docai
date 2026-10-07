package it.docai.documento;

import java.util.UUID;

public class DocumentoNonTrovatoException extends RuntimeException{

    private final UUID id;

    public DocumentoNonTrovatoException(UUID id) {
        super("Documento non trovato: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
