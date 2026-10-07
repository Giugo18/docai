package it.docai.documento;

public class UploadNonValidoException extends RuntimeException{
    private final ErroreUpload errore;

    public UploadNonValidoException(ErroreUpload errore) {
        super("Upload non valido: " + errore);
        this.errore = errore;
    }

    public ErroreUpload getErrore() {
        return errore;
    }
}
