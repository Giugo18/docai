package it.docai.consumo;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import it.docai.strumenti.ContestoStrumenti;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Registra i token di OGNI chiamata al modello di chat, chiunque la faccia (assistente, agente, estrazione). */
@Component
class ConsumoObservationHandler implements ObservationHandler<ChatModelObservationContext> {

    private final ConsumoService consumi;

    ConsumoObservationHandler(ConsumoService consumi) {
        this.consumi = consumi;
    }

    @Override
    public boolean supportsContext(Observation.Context contesto) {
        return contesto instanceof ChatModelObservationContext;
    }

    @Override
    public void onStop(ChatModelObservationContext contesto) {
        Map<String, Object> tool = contesto.getRequest().getOptions() instanceof ToolCallingChatOptions opzioni
                && opzioni.getToolContext() != null ? opzioni.getToolContext() : Map.of();
        // chi: dal ToolContext (vale anche in streaming, su altri thread), altrimenti dal JWT della richiesta
        String proprietario = tool.get(ContestoStrumenti.PROPRIETARIO) instanceof String p ? p : utenteCorrente();
        String operazione = tool.get(ConsumoService.CHIAVE_OPERAZIONE) instanceof String o ? o : "CHAT";
        consumi.registra(proprietario, operazione, contesto.getResponse());
    }

    private static String utenteCorrente() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a instanceof JwtAuthenticationToken jwt ? jwt.getToken().getSubject() : null;
    }
}