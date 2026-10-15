package it.docai.estrazione;

import java.util.regex.Pattern;

/** Pulizia del testo estratto dai PDF: meno token inutili, ricerca testuale più affidabile. */
public final class Testo {

    // invisibili: zero-width space, joiner, word joiner, BOM e trattino morbido (U+00AD, frequente nei PDF impaginati)
    private static final Pattern INVISIBILI = Pattern.compile("[\\u200B-\\u200D\\u2060\\uFEFF\\u00AD]");
    private static final Pattern A_CAPO = Pattern.compile("\\r\\n?");          // Windows (\r\n) e vecchio Mac (\r) → \n
    private static final Pattern SPAZI = Pattern.compile("\\h+");              // spazi, tab, NBSP, U+202F… → uno spazio
    private static final Pattern SPAZI_A_FINE_RIGA = Pattern.compile(" \\n");
    private static final Pattern RIGHE_VUOTE = Pattern.compile("\\n{3,}");     // più di una riga vuota → una sola

    private Testo() {
    }

    public static String normalizza(String testo) {
        if (testo == null) {
            return null;
        }
        String t = INVISIBILI.matcher(testo).replaceAll("");
        t = A_CAPO.matcher(t).replaceAll("\n");
        t = SPAZI.matcher(t).replaceAll(" ");
        t = SPAZI_A_FINE_RIGA.matcher(t).replaceAll("\n");
        t = RIGHE_VUOTE.matcher(t).replaceAll("\n\n");
        return t.strip();
    }
}