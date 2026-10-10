package it.docai.estrazione;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestoDaEstrarre(
        @NotBlank @Size(max = EstrazioneService.CARATTERI_MASSIMI) String testo) {
}