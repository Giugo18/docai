package it.docai.rag;

import java.util.List;

public record CasoDiProva(String domanda, String documento, List<String> paroleAttese, Boolean nonDeveTrovare) {
    public CasoDiProva {
        paroleAttese = paroleAttese == null ? List.of() : paroleAttese;
        nonDeveTrovare = nonDeveTrovare != null && nonDeveTrovare;     // assente nel JSON = false
    }
}
