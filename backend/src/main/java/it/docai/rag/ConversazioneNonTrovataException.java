package it.docai.rag;

import java.util.UUID;

public class ConversazioneNonTrovataException extends RuntimeException {
    public ConversazioneNonTrovataException(UUID id) {
        super("Conversazione non trovata: " + id);
    }
}
