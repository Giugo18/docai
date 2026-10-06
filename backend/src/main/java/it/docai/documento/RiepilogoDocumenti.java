package it.docai.documento;

import java.util.Map;

public record RiepilogoDocumenti(long totale, long dimensioneTotale, Map<StatoDocumento, Long> perStato) {
    // TODO: costruttore compatto che rifiuta totale o dimensioneTotale negativi

    public RiepilogoDocumenti {
        if (totale < 0) {
            throw new IllegalArgumentException("totale non può essere negativo: " + totale);
        }
        if (dimensioneTotale < 0) {
            throw new IllegalArgumentException("dimensioneTotale non può essere negativa: " + dimensioneTotale);
        }
        perStato = Map.copyOf(perStato);
    }
}
