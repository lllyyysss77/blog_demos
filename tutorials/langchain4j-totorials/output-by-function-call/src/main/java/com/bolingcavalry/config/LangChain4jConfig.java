package com.bolingcavalry.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bolingcavalry.service.Assistant;
import com.bolingcavalry.tool.HistoryEventTool;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

/**
 * LangChain4j配置类
 */
@Configuration
public class LangChain4jConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name:qwen-turbo}")
    private String modelName;

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String baseUrl;

    /**
     * 创建并配置OpenAiChatModel实例（使用通义千问的OpenAI兼容接口）
     * 
     * @return OpenAiChatModel实例
     */
    @Bean
    public OpenAiChatModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    public Assistant assistant(OpenAiChatModel chatModel, HistoryEventTool historyEventTool) {
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .tools(historyEventTool)
                .build();
    }

}