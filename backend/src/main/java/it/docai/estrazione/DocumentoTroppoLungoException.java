package it.docai.estrazione;

public class DocumentoTroppoLungoException extends RuntimeException {

    public DocumentoTroppoLungoException(int caratteri, int massimo) {
        super("Il documento contiene %d caratteri: il massimo per l'estrazione è %d."
                .formatted(caratteri, massimo));
    }
}