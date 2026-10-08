package it.docai.documento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RinominaDocumento(
        @NotBlank(message = "Il nome non può essere vuoto")
        @Size(max = 255, message = "Il nome può avere al massimo 255 caratteri")
        @Pattern(regexp = "(?i)\\s*|.+\\.pdf", message = "Il nome deve terminare con .pdf")
        String nomeFile
) {}