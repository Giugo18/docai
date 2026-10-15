package it.docai.estrazione;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestoTest {

    @Test
    void aCapoWindowsDiventanoUnix() {
        assertThat(Testo.normalizza("Riga uno\r\nRiga due\rRiga tre")).isEqualTo("Riga uno\nRiga due\nRiga tre");
    }

    @Test
    void spaziStraniETabDiventanoUnoSpazio() {
        assertThat(Testo.normalizza("Totale\t\t451,40\u00A0€   IVA\u202Finclusa")).isEqualTo("Totale 451,40 € IVA inclusa");
    }

    @Test
    void caratteriInvisibiliSpariscono() {
        assertThat(Testo.normalizza("bge\u200B-m3 e im\u00ADpaginato")).isEqualTo("bge-m3 e impaginato");
    }

    @Test
    void leRigheRestanoRigheMaLeRigheVuoteSiCompattano() {
        assertThat(Testo.normalizza("Biglietti 60,00   \r\n\r\n\r\n\r\nVolantini 160,00\r\n"))
                .isEqualTo("Biglietti 60,00\n\nVolantini 160,00");
    }

    @Test
    void nullResta_null() {
        assertThat(Testo.normalizza(null)).isNull();
    }
}