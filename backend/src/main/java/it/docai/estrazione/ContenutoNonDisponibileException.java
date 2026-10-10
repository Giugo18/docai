package it.docai.estrazione;

import java.util.UUID;

public class ContenutoNonDisponibileException extends RuntimeException {
    public ContenutoNonDisponibileException(UUID id) {
        super("Il file del documento " + id + " non è disponibile: caricalo di nuovo");
    }
}