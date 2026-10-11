package it.docai.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record NuovaConversazione(@NotBlank @Size(max = 200) String titolo, UUID documentoId) {}
