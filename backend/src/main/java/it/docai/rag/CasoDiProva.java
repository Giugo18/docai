package it.docai.rag;

import java.util.List;

public record CasoDiProva(String domanda, String documento, List<String> paroleAttese, Boolean nonDeveTrovare,
                          Modalita modalita, String utente, List<String> almenoUna, List<String> nonDeveContenere,
                          Integer maxPassi) {

    public enum Modalita { RAG, ASSISTENTE, AGENTE }

    public CasoDiProva {
        paroleAttese = paroleAttese == null ? List.of() : paroleAttese;
        nonDeveTrovare = nonDeveTrovare != null && nonDeveTrovare;     // assente nel JSON = false
        modalita = modalita == null ? Modalita.RAG : modalita;         // i casi vecchi restano RAG
        almenoUna = almenoUna == null ? List.of() : almenoUna;
        nonDeveContenere = nonDeveContenere == null ? List.of() : nonDeveContenere;
        // maxPassi assente = null: nessun limite da controllare
    }

    public boolean utenteVuoto() {
        return "vuoto".equals(utente);
    }

    /** Domanda con il percorso e l'utente, per distinguere i casi con lo stesso testo. */
    public String etichetta() {
        return (modalita == Modalita.RAG ? "" : "[" + modalita + "] ") + (utenteVuoto() ? "[vuoto] " : "") + domanda;
    }
}