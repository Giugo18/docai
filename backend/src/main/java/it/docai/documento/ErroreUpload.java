package it.docai.documento;

public sealed interface ErroreUpload {
    //record Valido() implements ErroreUpload {}
    record FileVuoto() implements ErroreUpload {}
    record TipoNonAmmesso(String contentType) implements ErroreUpload {}
    record TroppoGrande(long dimensione, long massimo) implements ErroreUpload {}
}