package it.docai.strumenti;

import it.docai.rag.Citazioni;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class AssistenteService {

    private final ChatClient chatClient;
    private final StrumentiDocumenti strumentiDocumenti;
    private final String regole;
    private final StrumentiRicerca strumentiRicerca;   // nel costruttore, come strumentiDocumenti


    public AssistenteService(ChatClient chatClient, StrumentiDocumenti strumentiDocumenti,
                             @Value("classpath:prompts/strumenti.st") Resource prompt,
                             StrumentiRicerca strumentiRicerca) throws IOException {
        this.chatClient = chatClient;
        this.strumentiDocumenti = strumentiDocumenti;
        this.regole = prompt.getContentAsString(StandardCharsets.UTF_8);
        this.strumentiRicerca = strumentiRicerca;
    }

    public String chiedi(String domanda, String proprietario) {
        String risposta = chatClient.prompt()
                .system(regole)
                .user(domanda)
                .tools(strumentiDocumenti, strumentiRicerca)
                .toolContext(Map.of(StrumentiDocumenti.PROPRIETARIO, proprietario))
                .call()
                .content();
        return Citazioni.normalizza(risposta);
    }
}