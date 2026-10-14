package it.docai.consumo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class ConsumoService {

    public enum Operazione { ASSISTENTE, AGENTE, ESTRAZIONE, RAG }

    private static final Logger log = LoggerFactory.getLogger(ConsumoService.class);
    private final JdbcClient jdbc;

    public ConsumoService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Registra i token di una risposta del modello. Non deve mai far fallire la richiesta dell'utente. */
    public void registra(String proprietario, Operazione operazione, ChatResponse risposta) {
        try {
            if (proprietario == null || risposta == null || risposta.getMetadata() == null) return;
            var uso = risposta.getMetadata().getUsage();
            if (uso == null || uso.getPromptTokens() == null) return;   // es. modello che non riporta l'uso
            String modello = risposta.getMetadata().getModel();
            jdbc.sql("""
                    INSERT INTO consumo (proprietario, operazione, modello, token_in, token_out)
                    VALUES (?, ?, ?, ?, ?)""")
                    .params(proprietario, operazione.name(), modello != null ? modello : "sconosciuto",
                            uso.getPromptTokens(), uso.getCompletionTokens() != null ? uso.getCompletionTokens() : 0)
                    .update();
        } catch (RuntimeException e) {
            log.warn("Consumo non registrato per {} ({}): {}", proprietario, operazione, e.getMessage());
        }
    }
}