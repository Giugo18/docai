package it.docai.rag;

import java.time.Instant;
import java.util.List;

public record MessaggioDto(RuoloMessaggio ruolo, String testo, List<Fonte> fonti, Instant creatoIl) {}

