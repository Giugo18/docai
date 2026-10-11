package it.docai.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DomandaInConversazione(@NotBlank @Size(max = 1000) String domanda) {}
