package it.docai.rag;

import java.util.regex.Pattern;

public final class Citazioni {

    // 【Fonte 2】, [[Fonte 2]], [ Fonte 2 ], 【【Fonte 2】】 → [Fonte 2]
    private static final Pattern CITAZIONE = Pattern.compile(
            "[\\[【]+\\s*(?:fonte\\s*)?(\\d+)(?:\\s*†[^\\]】]*)?\\s*[\\]】]+",
            Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE);
    private Citazioni() {
    }

    public static String normalizza(String testo) {
        return testo == null ? null : CITAZIONE.matcher(testo).replaceAll("[Fonte $1]");
    }
}