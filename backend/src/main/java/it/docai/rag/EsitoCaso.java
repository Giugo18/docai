package it.docai.rag;

import java.util.Set;

public record EsitoCaso(String domanda, boolean fonteTrovata, Integer posizione, boolean fonteCitata,
                        Set<Integer> citazioniInventate, boolean rispostaCorretta, long millisecondi,
                        Integer passi, String risposta) {

    public boolean citazioniValide() {
        return citazioniInventate.isEmpty();
    }

    /** Il caso non ha avuto risposta (eccezione): conta come sbagliato, con il motivo nel testo. */
    public static EsitoCaso errore(String domanda, String motivo, long millisecondi) {
        return new EsitoCaso(domanda, false, null, false, Set.of(), false, millisecondi, null, "ERRORE: " + motivo);
    }
}