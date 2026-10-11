package it.docai.estrazione;

public class TestoNonEstraibileException extends RuntimeException {
    public TestoNonEstraibileException(String messaggio) {
        super(messaggio);
    }

    public TestoNonEstraibileException(String messaggio, Throwable causa) {
        super(messaggio, causa);
    }
}