package it.docai.estrazione;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.math.BigDecimal;

public record RigaFattura(
        @JsonPropertyDescription("Descrizione del bene o servizio")
        String descrizione,
        @JsonPropertyDescription("Quantità, come numero")
        BigDecimal quantita,
        @JsonPropertyDescription("Prezzo unitario senza IVA")
        BigDecimal prezzoUnitario,
        @JsonPropertyDescription("Importo della riga senza IVA")
        BigDecimal importo) {
}