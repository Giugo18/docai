package it.docai.estrazione;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestoDaEstrarre(
        @NotBlank(message = "Il testo non può essere vuoto")
        @Size(max = 20000, message = "Il testo può avere al massimo 20000 caratteri")
        String testo) {
}