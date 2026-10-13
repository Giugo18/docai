package it.docai.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class McpConfig {

    @Bean
    ToolCallbackProvider strumentiMcpProvider(StrumentiMcp strumenti) {
        return MethodToolCallbackProvider.builder().toolObjects(strumenti).build();
    }
}