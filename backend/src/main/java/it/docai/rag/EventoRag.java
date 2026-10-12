package it.docai.rag;

import java.util.List;

public record EventoRag(String tipo, String testo, List<Fonte> fonti) {

    public static EventoRag fonti(List<Fonte> fonti) {
        return new EventoRag("fonti", null, fonti);
    }

    public static EventoRag testo(String testo) {
        return new EventoRag("testo", testo, null);
    }

    public static EventoRag riscritta(String domanda) {
        return new EventoRag("riscritta", domanda, null);
    }
}