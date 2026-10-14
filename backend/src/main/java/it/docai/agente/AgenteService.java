package it.docai.agente;

import it.docai.rag.Citazioni;
import it.docai.rag.Fonte;
import it.docai.rag.Proposta;
import it.docai.strumenti.ContestoStrumenti;
import it.docai.strumenti.StrumentiDocumenti;
import it.docai.strumenti.StrumentiRicerca;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AgenteService {

    private static final Logger log = LoggerFactory.getLogger(AgenteService.class);
    static final int PASSI_MASSIMI = 6;

    private final ChatModel chatModel;
    private final ToolCallingManager toolCallingManager;
    private final StrumentiDocumenti strumentiDocumenti;
    private final StrumentiRicerca strumentiRicerca;
    private final String istruzioni;

    public AgenteService(ChatModel chatModel, ToolCallingManager toolCallingManager,
                         StrumentiDocumenti strumentiDocumenti, StrumentiRicerca strumentiRicerca,
                         @Value("classpath:prompts/agente.st") Resource prompt) throws IOException {
        this.chatModel = chatModel;
        this.toolCallingManager = toolCallingManager;
        this.strumentiDocumenti = strumentiDocumenti;
        this.strumentiRicerca = strumentiRicerca;
        this.istruzioni = prompt.getContentAsString(StandardCharsets.UTF_8);
    }

    public RispostaAgente esegui(String compito, String proprietario) {
        List<Fonte> fonti = new CopyOnWriteArrayList<>();
        List<Proposta> proposte = new CopyOnWriteArrayList<>();   // resta vuota: l'agente non ha tool di modifica

        // Spring AI 2.0: OpenAiChatModel vuole OpenAiChatOptions. Parto dalle opzioni del modello
        // (modello, max token, extra-body di application.yml) e aggiungo solo tool e contesto.
        if (!(chatModel.getDefaultOptions().mutate() instanceof ToolCallingChatOptions.Builder builder)) {
            throw new IllegalStateException("Il modello configurato non supporta i tool");
        }
        ChatOptions opzioni = builder
                .toolCallbacks(MethodToolCallbackProvider.builder()
                        .toolObjects(strumentiDocumenti, strumentiRicerca).build().getToolCallbacks())
                .toolContext(ContestoStrumenti.di(proprietario, null, fonti, proposte))
                .build();

        Prompt prompt = new Prompt(List.<Message>of(new SystemMessage(istruzioni), new UserMessage(compito)), opzioni);
        ChatResponse risposta = chiama(prompt, 0);

        int passi = 0;
        List<String> strumentiUsati = new ArrayList<>();
        while (risposta.hasToolCalls() && passi < PASSI_MASSIMI) {
            passi++;
            risposta.getResult().getOutput().getToolCalls()
                    .forEach(t -> strumentiUsati.add(t.name() + " " + t.arguments()));
            ToolExecutionResult esito = toolCallingManager.executeToolCalls(prompt, risposta);
            prompt = new Prompt(esito.conversationHistory(), opzioni);
            risposta = chiama(prompt, passi);
        }

        boolean limiteRaggiunto = risposta.hasToolCalls();
        if (limiteRaggiunto) {
            log.warn("Agente: limite di {} passi raggiunto per '{}'", PASSI_MASSIMI, compito);
            risposta = chiudi(prompt);
        }
        String testo = Citazioni.normalizza(risposta.getResult().getOutput().getText());
        return new RispostaAgente(testo, passi, limiteRaggiunto, strumentiUsati, List.copyOf(fonti));
    }

    /** Un giro con il modello, con il log dei token: in un agente i costi crescono a ogni passo. */
    private ChatResponse chiama(Prompt prompt, int passo) {
        ChatResponse risposta = chatModel.call(prompt);
        var uso = risposta.getMetadata().getUsage();
        log.info("Agente passo {}: token in {} / out {}, tool richiesti: {}",
                passo, uso.getPromptTokens(), uso.getCompletionTokens(), risposta.hasToolCalls());
        return risposta;
    }

    /**
     * Tetto raggiunto: un'ultima chiamata SENZA tool, chiedendo una risposta onesta con quello che c'è.
     * Uso il prompt dell'ultimo giro, che finisce con le risposte dei tool: la richiesta di tool
     * rimasta in sospeso non entra nella cronologia, altrimenti l'API la rifiuterebbe.
     */
    private ChatResponse chiudi(Prompt ultimo) {
        List<Message> messaggi = new ArrayList<>(ultimo.getInstructions());
        messaggi.add(new UserMessage("""
                Hai raggiunto il numero massimo di passaggi. Non puoi più usare strumenti.
                Rispondi ora con le informazioni che hai già, e di' chiaramente cosa non sei riuscito a verificare."""));
        return chiama(new Prompt(messaggi, chatModel.getDefaultOptions()), PASSI_MASSIMI + 1);    }
}