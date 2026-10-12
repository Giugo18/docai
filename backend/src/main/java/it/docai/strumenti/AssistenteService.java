package it.docai.strumenti;

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

    public AssistenteService(ChatClient chatClient, StrumentiDocumenti strumentiDocumenti,
                             @Value("classpath:prompts/strumenti.st") Resource prompt) throws IOException {
        this.chatClient = chatClient;
        this.strumentiDocumenti = strumentiDocumenti;
        this.regole = prompt.getContentAsString(StandardCharsets.UTF_8);
    }

    public String chiedi(String domanda, String proprietario) {
        return chatClient.prompt()
                .system(regole)
                .user(domanda)
                .tools(strumentiDocumenti)                                         // i metodi @Tool di questo bean
                .toolContext(Map.of(StrumentiDocumenti.PROPRIETARIO, proprietario)) // dal JWT, invisibile al modello
                .call()
                .content();
    }
}