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
}