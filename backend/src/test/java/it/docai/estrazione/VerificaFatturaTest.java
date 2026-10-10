package it.docai.estrazione;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VerificaFatturaTest {

    private final VerificaFattura verifica = new VerificaFattura();   // niente Spring: una semplice new

    @Test
    void fatturaCoerenteNonHaProblemi() {
        assertThat(verifica.problemi(fattura("451.40"))).isEmpty();
    }

    @Test
    void totaleCheNonTornaVieneSegnalato() {
        assertThat(verifica.problemi(fattura("999.00")))
                .containsExactly("Imponibile + IVA (451.40) non corrisponde al totale (999.00)");
    }

    @Test
    void unCentesimoDiArrotondamentoEAmmesso() {
        assertThat(verifica.problemi(fattura("451.41"))).isEmpty();
    }

    @Test
    void campiObbligatoriMancanti() {
        var vuota = new Fattura(null, null, null, null, null, List.of(), null, null, null);
        assertThat(verifica.problemi(vuota)).containsExactlyInAnyOrder(
                "Manca il numero della fattura",
                "Manca la data della fattura",
                "Manca il fornitore",
                "Manca il cliente",
                "Manca il totale");
    }

    @Test
    void rigaConImportoSbagliato() {
        var righe = List.of(new RigaFattura("Volantini", new BigDecimal("2000"), new BigDecimal("0.08"), new BigDecimal("170.00")));
        var f = new Fattura("1", LocalDate.of(2026, 10, 3), "Rossi", null, "Bianchi", righe,
                new BigDecimal("170.00"), new BigDecimal("37.40"), new BigDecimal("207.40"));
        assertThat(verifica.problemi(f))
                .containsExactly("Riga 1: quantità × prezzo unitario non corrisponde all'importo 170.00");
    }

    /** La fattura di prova con il totale indicato */
    private static Fattura fattura(String totale) {
        var righe = List.of(
                new RigaFattura("Stampa volantini A5 a colori", new BigDecimal("2000"), new BigDecimal("0.08"), new BigDecimal("160.00")),
                new RigaFattura("Biglietti da visita", new BigDecimal("500"), new BigDecimal("0.12"), new BigDecimal("60.00")),
                new RigaFattura("Progetto grafico", new BigDecimal("1"), new BigDecimal("150.00"), new BigDecimal("150.00")));
        return new Fattura("2026/0457", LocalDate.of(2026, 10, 3), "Tipografia Rossi S.r.l.", "02345678901",
                "Studio Bianchi", righe, new BigDecimal("370.00"), new BigDecimal("81.40"), new BigDecimal(totale));
    }
}