package it.docai.estrazione;

import java.util.List;

/** Fattura estratta, problemi rimasti dopo i tentativi, numero di tentativi fatti */
public record EsitoEstrazione(Fattura fattura, List<String> avvisi, int tentativi) {
}