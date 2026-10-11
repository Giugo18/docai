package it.docai.rag;

public record Fonte(int numero, String documentoId, String nomeFile, int pagina,
                    double punteggio, String estratto) {
}
