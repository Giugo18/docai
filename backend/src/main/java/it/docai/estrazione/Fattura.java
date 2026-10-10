package it.docai.estrazione;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Fattura(
        @JsonPropertyDescription("Numero della fattura così come scritto nel documento")
        String numero,
        @JsonPropertyDescription("Data di emissione")
        LocalDate data,
        @JsonPropertyDescription("Ragione sociale di chi emette la fattura")
        String fornitore,
        @JsonPropertyDescription("Partita IVA del fornitore, solo cifre")
        String partitaIvaFornitore,
        @JsonPropertyDescription("Nome di chi riceve la fattura")
        String cliente,
        @JsonPropertyDescription("Righe della fattura")
        List<RigaFattura> righe,
        @JsonPropertyDescription("Totale imponibile, senza IVA")
        BigDecimal imponibile,
        @JsonPropertyDescription("Importo totale dell'IVA")
        BigDecimal iva,
        @JsonPropertyDescription("Totale da pagare, IVA inclusa")
        BigDecimal totale) {
}