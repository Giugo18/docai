package it.docai.documento;

public sealed interface EsitoValidazione {
    record Valido() implements EsitoValidazione {}
    record FileVuoto() implements EsitoValidazione {}
    record TipoNonAmmesso(String contentType) implements EsitoValidazione {}
    record TroppoGrande(long dimensione, long massimo) implements EsitoValidazione {}
}