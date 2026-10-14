package it.docai.consumo;

import java.math.BigDecimal;
import java.util.List;

public record RiepilogoConsumi(String mese, String valuta, List<ConsumoUtente> righe, BigDecimal totale) { }
