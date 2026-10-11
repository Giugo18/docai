package it.docai.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DomandaDocumenti(
        @NotBlank @Size(max = 1000) String domanda,
        UUID documentoId) {          // facoltativo: null = tutti i miei documenti
}