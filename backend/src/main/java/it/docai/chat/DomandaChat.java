package it.docai.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DomandaChat(
        @NotBlank(message = "La domanda non può essere vuota")
        @Size(max = 2000, message = "La domanda può avere al massimo 2000 caratteri")
        String domanda) {
}