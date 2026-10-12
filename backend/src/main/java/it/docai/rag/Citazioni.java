package it.docai.rag;

import java.util.regex.Pattern;

final class Citazioni {

    // 【Fonte 2】, [[Fonte 2]], [ Fonte 2 ], 【【Fonte 2】】 → [Fonte 2]
    private static final Pattern CITAZIONE =
            Pattern.compile("[\\[【]+\\s*Fonte\\s*(\\d+)\\s*[\\]】]+", Pattern.UNICODE_CHARACTER_CLASS);

    private Citazioni() {
    }

    static String normalizza(String testo) {
        return testo == null ? null : CITAZIONE.matcher(testo).replaceAll("[Fonte $1]");
    }
}