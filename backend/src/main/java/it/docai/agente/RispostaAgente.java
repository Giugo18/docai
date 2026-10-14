package it.docai.agente;

import it.docai.rag.Fonte;
import java.util.List;

/** Esito di un compito dell'agente: la risposta più i dati per capire come ci è arrivato. */
public record RispostaAgente(
        String risposta,
        int passi,                       // quante volte il modello ha chiesto dei tool
        boolean limiteRaggiunto,         // true = si è fermato per il tetto, non perché aveva finito
        List<String> strumentiUsati,     // es. "cercaNeiDocumenti {"domanda":"totale ..."}"
        List<Fonte> fonti) {
}