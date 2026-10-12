package it.docai.rag;

import java.util.List;

public record EventoRag(String tipo, String testo, List<Fonte> fonti, Proposta proposta) {

    public static EventoRag fonti(List<Fonte> fonti)  { return new EventoRag("fonti", null, fonti, null); }
    public static EventoRag testo(String testo)       { return new EventoRag("testo", testo, null, null); }
    public static EventoRag riscritta(String testo)   { return new EventoRag("riscritta", testo, null, null); }
    public static EventoRag proposta(Proposta p)      { return new EventoRag("proposta", null, null, p); }
}