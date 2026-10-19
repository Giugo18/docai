package it.docai.rag;

import java.util.List;

public record CasoDiProva(String domanda, String documento, List<String> paroleAttese, Boolean nonDeveTrovare,
                          Modalita modalita, String utente, List<String> almenoUna, List<String> nonDeveContenere,
                          Integer maxPassi, List<String> prima, Integer ripeti) {

    public enum Modalita { RAG, ASSISTENTE, AGENTE }

    public CasoDiProva {
        paroleAttese = paroleAttese == null ? List.of() : paroleAttese;
        nonDeveTrovare = nonDeveTrovare != null && nonDeveTrovare;     // assente nel JSON = false
        modalita = modalita == null ? Modalita.RAG : modalita;         // i casi vecchi restano RAG
        almenoUna = almenoUna == null ? List.of() : almenoUna;
        nonDeveContenere = nonDeveContenere == null ? List.of() : nonDeveContenere;
        // maxPassi assente = null: nessun limite da controllare
        prima = prima == null ? List.of() : prima;                    // domande precedenti nella stessa conversazione
        ripeti = ripeti == null ? 1 : ripeti;                          // quante volte eseguire il caso
        if (ripeti < 1) {
            throw new IllegalArgumentException("ripeti deve essere almeno 1: " + domanda);
        }
        if (!prima.isEmpty() && modalita != Modalita.ASSISTENTE) {
            throw new IllegalArgumentException("'prima' vale solo in modalità ASSISTENTE (l'unica con la conversazione): " + domanda);
        }
    }

    public boolean utenteVuoto() {
        return "vuoto".equals(utente);
    }

    /** Domanda con il percorso, l'utente e la conversazione, per distinguere i casi con lo stesso testo. */
    public String etichetta() {
        return (modalita == Modalita.RAG ? "" : "[" + modalita + "] ")
                + (utenteVuoto() ? "[vuoto] " : "")
                + (prima.isEmpty() ? "" : "[dopo: " + String.join(" | ", prima) + "] ")
                + domanda;
    }
}