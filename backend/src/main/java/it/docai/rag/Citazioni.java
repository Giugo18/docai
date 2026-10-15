package it.docai.rag;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Citazioni {

    private static final Pattern NORMALIZZATA = Pattern.compile("\\[Fonte (\\d+)]");
    private static final Pattern INVISIBILI = Pattern.compile("[\\u200B-\\u200D\\u2060\\uFEFF]");



    // 【Fonte 2】, [[Fonte 2]], [ Fonte 2 ], 【【Fonte 2】】 → [Fonte 2]
    private static final Pattern CITAZIONE = Pattern.compile(
            "[\\[【]+\\s*(?:fonte\\s*)?(\\d+)(?:\\s*†[^\\]】]*)?\\s*[\\]】]+",
            Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE);
    private Citazioni() {
    }

    public static String normalizza(String testo) {
        if (testo == null) {
            return null;
        }
        String pulito = INVISIBILI.matcher(testo).replaceAll("");
        return CITAZIONE.matcher(pulito).replaceAll("[Fonte $1]");
    }

    /** I numeri citati nel testo, in ordine di prima comparsa e senza ripetizioni. Accetta anche testo non normalizzato. */
    public static Set<Integer> numeri(String testo) {
        var numeri = new LinkedHashSet<Integer>();
        if (testo == null) {
            return numeri;
        }
        Matcher m = NORMALIZZATA.matcher(normalizza(testo));
        while (m.find()) {
            numeri.add(Integer.parseInt(m.group(1)));
        }
        return numeri;
    }

    /** Le citazioni che non corrispondono a nessuna fonte restituita: il modello le ha inventate. */
    public static Set<Integer> inventate(String testo, Collection<Integer> numeriFonti) {
        var inventate = numeri(testo);
        inventate.removeAll(numeriFonti);
        return inventate;
    }
}