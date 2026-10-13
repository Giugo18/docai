package it.docai.mcp;

import it.docai.documento.DocumentoService;
import it.docai.strumenti.StrumentiDocumenti;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;

@Component
public class StrumentiMcp {

    private final DocumentoService documenti;

    public StrumentiMcp(DocumentoService documenti) {
        this.documenti = documenti;
    }

    @Tool(description = "Elenca i documenti PDF dell'utente autenticato: id, nome del file, stato di indicizzazione e data di caricamento.")
    public List<StrumentiDocumenti.DocumentoInBreve> elencaDocumenti() {
        return documenti.elenca(proprietario()).stream()
                .map(StrumentiDocumenti.DocumentoInBreve::da)
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