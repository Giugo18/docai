package it.docai.chat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class ChatConfig {
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 @Value("classpath:prompts/assistente.st") Resource promptSistema) throws IOException {
        String testoSistema = promptSistema.getContentAsString(StandardCharsets.UTF_8);
        return builder
                .defaultSystem(testoSistema)   // messaggio "system" aggiunto a ogni richiesta
                .build();
    }
}
