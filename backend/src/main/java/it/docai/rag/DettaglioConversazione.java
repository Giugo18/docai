package it.docai.rag;

import java.util.List;

public record DettaglioConversazione(ConversazioneDto conversazione, List<MessaggioDto> messaggi) {}
