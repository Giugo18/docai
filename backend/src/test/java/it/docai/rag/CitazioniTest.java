package it.docai.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CitazioniTest {

    @Test
    void trovaINumeriInTutteLeFormeSenzaRipetizioni() {
        String testo = "Totale [Fonte 1], dettaglio 【fonte 3】, ancora [[Fonte 1]] e 【5†Tipografia】.";
        assertThat(Citazioni.numeri(testo)).containsExactly(1, 3, 5);
    }

    @Test
    void segnalaLeCitazioniCheNonHannoUnaFonte() {
        String testo = "Il totale è 451,40 € [Fonte 2], pagato il 10/10 [Fonte 5].";
        assertThat(Citazioni.inventate(testo, List.of(1, 2, 3, 4))).containsExactly(5);
    }

    @Test
    void nessunaCitazioneNessunProblema() {
        assertThat(Citazioni.numeri("Non trovo questa informazione nei tuoi documenti.")).isEmpty();
        assertThat(Citazioni.numeri(null)).isEmpty();
        assertThat(Citazioni.inventate("Senza citazioni", List.of())).isEmpty();
    }

    @Test
    void ignoraICaratteriInvisibili() {
        // U+200B (zero-width space) dopo la parentesi e U+202F tra "Fonte" e il numero: visto con gpt-oss
        String testo = "Totale 451,40 € [\u200BFonte\u202F1].";
        assertThat(Citazioni.normalizza(testo)).isEqualTo("Totale 451,40 € [Fonte 1].");
        assertThat(Citazioni.numeri(testo)).containsExactly(1);
    }

    @Test
    void riconosceLeCitazioniInSpagnoloEInglese() {
        assertThat(Citazioni.normalizza("il cerchio [Fuente 3] e la storia [Source 4]"))
                .isEqualTo("il cerchio [Fonte 3] e la storia [Fonte 4]");
        assertThat(Citazioni.numeri("vedi [Fuente 3]")).containsExactly(3);
    }
}