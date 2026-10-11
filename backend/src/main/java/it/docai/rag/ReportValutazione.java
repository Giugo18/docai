package it.docai.rag;

import java.util.List;

public record ReportValutazione(int casi, int fontiTrovate, int risposteCorrette,
                                long tempoMedioMs, List<EsitoCaso> esiti) {}