package it.docai.estrazione;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class VerificaFattura {

    private static final BigDecimal TOLLERANZA = new BigDecimal("0.01");   // un centesimo di arrotondamento

    /** Elenco dei problemi trovati: vuoto = fattura coerente */
    public List<String> problemi(Fattura f) {
        if (f == null) {
            return List.of("Il modello non ha restituito una fattura");
        }
        var problemi = new ArrayList<String>();

        if (vuoto(f.numero()))    problemi.add("Manca il numero della fattura");
        if (f.data() == null)     problemi.add("Manca la data della fattura");
        if (vuoto(f.fornitore())) problemi.add("Manca il fornitore");
        if (vuoto(f.cliente()))   problemi.add("Manca il cliente");
        if (f.totale() == null)   problemi.add("Manca il totale");

        var righe = f.righe() == null ? List.<RigaFattura>of() : f.righe();
        for (int i = 0; i < righe.size(); i++) {
            var r = righe.get(i);
            if (r.quantita() != null && r.prezzoUnitario() != null && r.importo() != null
                    && diversi(r.quantita().multiply(r.prezzoUnitario()), r.importo())) {
                problemi.add("Riga " + (i + 1) + ": quantità × prezzo unitario non corrisponde all'importo " + r.importo());
            }
        }

        if (!righe.isEmpty() && f.imponibile() != null) {
            BigDecimal somma = righe.stream()
                    .map(RigaFattura::importo)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (diversi(somma, f.imponibile())) {
                problemi.add("La somma delle righe (" + somma + ") non corrisponde all'imponibile (" + f.imponibile() + ")");
            }
        }

        if (f.imponibile() != null && f.iva() != null && f.totale() != null
                && diversi(f.imponibile().add(f.iva()), f.totale())) {
            problemi.add("Imponibile + IVA (" + f.imponibile().add(f.iva()) + ") non corrisponde al totale (" + f.totale() + ")");
        }
        return problemi;
    }

    private static boolean vuoto(String s) {
        return s == null || s.isBlank();
    }

    private static boolean diversi(BigDecimal a, BigDecimal b) {
        return a.subtract(b).abs().compareTo(TOLLERANZA) > 0;
    }
}