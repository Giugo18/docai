package it.docai.consumo;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/** Prezzi per milione di token, per modello. Un modello senza prezzo costa 0 (e lo si vede subito). */
@ConfigurationProperties("docai.prezzi")
public record PrezziModelli(String valuta, Map<String, Prezzo> modelli) {

    public record Prezzo(BigDecimal ingresso, BigDecimal uscita) { }

    private static final BigDecimal MILIONE = BigDecimal.valueOf(1_000_000);

    public BigDecimal costo(String modello, long tokenIn, long tokenOut) {
        Prezzo p = modelli == null ? null : modelli.get(modello);
        if (p == null) {
            return BigDecimal.ZERO;
        }
        return p.ingresso().multiply(BigDecimal.valueOf(tokenIn))
                .add(p.uscita().multiply(BigDecimal.valueOf(tokenOut)))
                .divide(MILIONE, 6, RoundingMode.HALF_UP);
    }
}