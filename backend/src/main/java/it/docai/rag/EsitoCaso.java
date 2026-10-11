package it.docai.rag;

public record EsitoCaso(String domanda, boolean fonteTrovata, Integer posizioneFonte,
                        boolean rispostaCorretta, long millisecondi, String risposta) {}
