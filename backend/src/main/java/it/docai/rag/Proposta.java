package it.docai.rag;

/** Una modifica suggerita dal modello: non è ancora fatta, la esegue solo l'utente confermando. */
public record Proposta(String tipo, String documentoId, String nomeAttuale, String nuovoNome) {

    public static Proposta rinomina(String documentoId, String nomeAttuale, String nuovoNome) {
        return new Proposta("RINOMINA", documentoId, nomeAttuale, nuovoNome);
    }
}