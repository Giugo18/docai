package it.docai.chat;

import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class ChatConfig {
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 ChatModel chatModel,
                                 @Value("classpath:prompts/assistente.st") Resource promptSistema) throws IOException {
        LoggerFactory.getLogger(ChatConfig.class)
                .info("Modello per generare il testo: {}", chatModel.getClass().getSimpleName());
        String testoSistema = promptSistema.getContentAsString(StandardCharsets.UTF_8);
        return builder
                .defaultSystem(testoSistema)
                .build();
    }
}
