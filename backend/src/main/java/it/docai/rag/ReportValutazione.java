package it.docai.rag;

import java.util.List;

public record ReportValutazione(int casi, int fontiTrovate, int fontiCitate, int citazioniValide,
                                int risposteCorrette, long tempoMedio, List<EsitoCaso> esiti) { }