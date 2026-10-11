package it.docai.rag;

import java.time.Instant;
import java.util.UUID;

public record ConversazioneDto(UUID id, String titolo, UUID documentoId, Instant aggiornataIl) {
    static ConversazioneDto da(Conversazione c) {
        return new ConversazioneDto(c.getId(), c.getTitolo(), c.getDocumentoId(), c.getAggiornataIl());
    }
}