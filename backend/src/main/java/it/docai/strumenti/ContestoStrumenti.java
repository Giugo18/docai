package it.docai.strumenti;

import it.docai.rag.Fonte;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Cosa passiamo ai tool dal codice: il modello non lo vede e non può cambiarlo. */
final class ContestoStrumenti {

    static final String PROPRIETARIO = "proprietario";
    static final String DOCUMENTO = "documentoId";
    static final String FONTI = "fonti";

    private ContestoStrumenti() {
    }

    static Map<String, Object> di(String proprietario, UUID documentoId, List<Fonte> fonti) {
        var contesto = new HashMap<String, Object>();       // Map.of non accetta null
        contesto.put(PROPRIETARIO, proprietario);
        contesto.put(FONTI, fonti);
        if (documentoId != null) {
            contesto.put(DOCUMENTO, documentoId);           // conversazione legata a un documento
        }
        return contesto;
    }
}