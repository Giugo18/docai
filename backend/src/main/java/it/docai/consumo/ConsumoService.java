package it.docai.consumo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Service
public class ConsumoService {

    //public enum Operazione { ASSISTENTE, AGENTE, ESTRAZIONE, RAG }
    public static final String CHIAVE_OPERAZIONE = "operazione";

    private static final Logger log = LoggerFactory.getLogger(ConsumoService.class);
    private final JdbcClient jdbc;
    private static final ZoneId FUSO = ZoneId.of("Europe/Rome");
    private final PrezziModelli prezzi;

    public ConsumoService(JdbcClient jdbc, PrezziModelli prezzi) {
        this.jdbc = jdbc;
        this.prezzi = prezzi;
    }

    /** Registra i token di una risposta del modello. Non deve mai far fallire la richiesta dell'utente. */
    public void registra(String proprietario, String utente, String operazione, ChatResponse risposta) {
        // chiamata fallita (chiave non valida, Groq irraggiungibile...): nessuna risposta, niente da registrare
        if (risposta == null || risposta.getMetadata() == null || risposta.getMetadata().getUsage() == null) {
            log.debug("Nessun consumo da registrare per {} ({}): risposta assente", proprietario, operazione);
            return;
        }
        try {
            var uso = risposta.getMetadata().getUsage();
            String modello = risposta.getMetadata().getModel();
            jdbc.sql("""
                    INSERT INTO consumo (proprietario, utente, operazione, modello, token_in, token_out)
                    VALUES (:proprietario, :utente, :operazione, :modello, :tokenIn, :tokenOut)""")
                    .param("proprietario", proprietario)
                    .param("utente", utente)
                    .param("operazione", operazione)
                    .param("modello", modello != null ? modello : "sconosciuto")
                    .param("tokenIn", uso.getPromptTokens() != null ? uso.getPromptTokens() : 0)
                    .param("tokenOut", uso.getCompletionTokens() != null ? uso.getCompletionTokens() : 0)
                    .update();
        } catch (RuntimeException e) {
            log.warn("Consumo non registrato per {} ({}): {}", proprietario, operazione, e.getMessage());
        }
    }

    public RiepilogoConsumi riepilogo(YearMonth mese) {
        OffsetDateTime inizio = mese.atDay(1).atStartOfDay(FUSO).toOffsetDateTime();
        OffsetDateTime fine = mese.plusMonths(1).atDay(1).atStartOfDay(FUSO).toOffsetDateTime();

        List<ConsumoUtente> righe = jdbc.sql("""
                SELECT c.proprietario,
                       (SELECT MAX(u.utente) FROM consumo u WHERE u.proprietario = c.proprietario) AS utente,
                       c.operazione, c.modello,
                       COUNT(*) AS chiamate, SUM(c.token_in) AS token_in, SUM(c.token_out) AS token_out
                FROM consumo c
                WHERE c.creato_il >= :inizio AND c.creato_il < :fine
                GROUP BY c.proprietario, c.operazione, c.modello
                ORDER BY utente, c.operazione""")
                .param("inizio", inizio)
                .param("fine", fine)
                .query((rs, n) -> {
                    long in = rs.getLong("token_in");
                    long out = rs.getLong("token_out");
                    String modello = rs.getString("modello");
                    return new ConsumoUtente(rs.getString("proprietario"), rs.getString("utente"),
                            rs.getString("operazione"), modello, rs.getLong("chiamate"), in, out,
                            prezzi.costo(modello, in, out));
                })
                .list();

        BigDecimal totale = righe.stream().map(ConsumoUtente::costoStimato).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RiepilogoConsumi(mese.toString(), prezzi.valuta(), righe, totale);
    }
}