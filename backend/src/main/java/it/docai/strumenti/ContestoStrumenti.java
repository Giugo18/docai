package it.docai.strumenti;

import it.docai.rag.Fonte;
import it.docai.rag.Proposta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Cosa passiamo ai tool dal codice: il modello non lo vede e non può cambiarlo. */
public final class ContestoStrumenti {

    public static final String PROPRIETARIO = "proprietario";
    static final String DOCUMENTO = "documentoId";
    static final String FONTI = "fonti";
    static final String PROPOSTE = "proposte";

    private ContestoStrumenti() {
    }

    public static Map<String, Object> di(String proprietario, UUID documentoId, List<Fonte> fonti) {
        var contesto = new HashMap<String, Object>();       // Map.of non accetta null
        contesto.put(PROPRIETARIO, proprietario);
        contesto.put(FONTI, fonti);
        if (documentoId != null) {
            contesto.put(DOCUMENTO, documentoId);           // conversazione legata a un documento
        }
        return contesto;
    }

    public static Map<String, Object> di(String proprietario, UUID documentoId,
                                  List<Fonte> fonti, List<Proposta> proposte) {
        var contesto = new HashMap<String, Object>();
        contesto.put(PROPRIETARIO, proprietario);
        contesto.put(FONTI, fonti);
        contesto.put(PROPOSTE, proposte);
        if (documentoId != null) {
            contesto.put(DOCUMENTO, documentoId);
        }
        return contesto;
    }
}