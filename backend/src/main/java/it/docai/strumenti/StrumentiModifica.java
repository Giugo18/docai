package it.docai.strumenti;

import it.docai.documento.DocumentoService;
import it.docai.rag.Proposta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class StrumentiModifica {

    private static final Logger log = LoggerFactory.getLogger(StrumentiModifica.class);
    private static final int NOME_MASSIMO = 200;

    private final DocumentoService documenti;

    public StrumentiModifica(DocumentoService documenti) {
        this.documenti = documenti;
    }

    @Tool(description = """
            Prepara la proposta di rinominare un documento dell'utente. NON rinomina: \
            l'utente vedrà un pulsante per confermare o annullare. Usalo solo se l'utente \
            chiede esplicitamente di rinominare un documento. Prendi l'id da elencaDocumenti.""")
    @SuppressWarnings("unchecked")
    public String proponiRinomina(
            @ToolParam(description = "id del documento, preso da elencaDocumenti") String documentoId,
            @ToolParam(description = "nuovo nome del file, con l'estensione") String nuovoNome,
            ToolContext contesto) {
        Map<String, Object> c = contesto.getContext();
        String proprietario = (String) c.get(ContestoStrumenti.PROPRIETARIO);
        List<Proposta> proposte = (List<Proposta>) c.get(ContestoStrumenti.PROPOSTE);
        log.info("Tool proponiRinomina {} → '{}' per {}", documentoId, nuovoNome, proprietario);

        if (nuovoNome == null || nuovoNome.isBlank() || nuovoNome.length() > NOME_MASSIMO) {
            return "Nome non valido: deve avere da 1 a " + NOME_MASSIMO + " caratteri.";
        }
        if (!nuovoNome.strip().toLowerCase().endsWith(".pdf")) {          // ← nuovo
            return "Nome non valido: deve terminare con .pdf.";
        }
        UUID id;
        try {
            id = UUID.fromString(documentoId);
        } catch (IllegalArgumentException e) {
            return "Id non valido: usa l'id restituito da elencaDocumenti.";
        }
        String nomeAttuale;
        try {
            nomeAttuale = documenti.trovaDelProprietario(id, proprietario).getNomeFile();   // 404 se non è suo
        } catch (RuntimeException e) {
            return "Documento non trovato tra quelli dell'utente.";
        }

        proposte.add(Proposta.rinomina(id.toString(), nomeAttuale, nuovoNome.strip()));
        return "Proposta pronta: rinominare '" + nomeAttuale + "' in '" + nuovoNome.strip()
                + "'. Il documento NON è ancora rinominato: l'utente deve premere Conferma.";
    }
}