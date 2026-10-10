package it.docai.sperimentale;

import jakarta.validation.constraints.NotBlank;

public record CoppiaDiFrasi(@NotBlank String a, @NotBlank String b) {
}