package it.docai.mcp;

import it.docai.documento.DocumentoService;
import it.docai.strumenti.StrumentiDocumenti;
import it.docai.strumenti.StrumentiRicerca;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;


import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.springframework.ai.document.Document;

@Component
public class StrumentiMcp {

    private static final Logger log = LoggerFactory.getLogger(StrumentiMcp.class);
    private final DocumentoService documenti;
    private final StrumentiRicerca ricerca;

    public StrumentiMcp(DocumentoService documenti, StrumentiRicerca ricerca) {
        this.documenti = documenti;
        this.ricerca = ricerca;
    }

    @Tool(description = "Elenca i documenti PDF dell'utente autenticato: id, nome del file, stato di indicizzazione e data di caricamento.")
    public List<StrumentiDocumenti.DocumentoInBreve> elencaDocumenti() {
        return documenti.elenca(proprietario()).stream()
                .map(StrumentiDocumenti.DocumentoInBreve::da)
                .toList();
    }

    @Tool(description = """
            Cerca nel contenuto dei documenti PDF dell'utente i passaggi più pertinenti a una domanda \
            (ricerca semantica e per parole). Restituisce fino a 4 passaggi con numero di fonte, \
            nome del file, pagina e testo. Per cercare in un solo documento passa il suo id, \
            ottenuto da elencaDocumenti.""")
    public List<StrumentiRicerca.Passaggio> cercaNeiDocumenti(
            @ToolParam(description = "Cosa cercare: una domanda completa e autonoma") String domanda,
            @ToolParam(required = false, description = "Id del documento in cui cercare; ometti per cercare in tutti")
            String documentoId) {
        String proprietario = proprietario();
        UUID id = (documentoId == null || documentoId.isBlank()) ? null : UUID.fromString(documentoId.strip());
        log.info("MCP cercaNeiDocumenti '{}' documento={} per {}", domanda, id, proprietario);

        List<Document> trovati = ricerca.cerca(domanda, id, proprietario);
        return IntStream.range(0, trovati.size())
                .mapToObj(i -> StrumentiRicerca.passaggio(i + 1, trovati.get(i)))
                .toList();
    }

    /** Il proprietario arriva dal JWT della richiesta /mcp, non dal ToolContext. */
    private static String proprietario() {
        Authentication autenticazione = SecurityContextHolder.getContext().getAuthentication();
        if (autenticazione instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getSubject();
        }
        throw new AccessDeniedException("Utente non autenticato");
    }
}