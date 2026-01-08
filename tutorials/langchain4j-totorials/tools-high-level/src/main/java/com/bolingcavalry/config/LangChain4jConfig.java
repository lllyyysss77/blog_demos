package com.bolingcavalry.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bolingcavalry.service.Assistant;
import com.bolingcavalry.service.WeatherTools;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.response.ChatResponse;
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
        ChatModelListener logger = new ChatModelListener() {
            @Override
            public void onRequest(ChatModelRequestContext reqCtx) {
                // 1. 拿到 List<ChatMessage>
                List<ChatMessage> messages = reqCtx.chatRequest().messages();
                System.out.println("→ 请求: " + messages);
            }

            @Override
            public void onResponse(ChatModelResponseContext respCtx) {
                // 2. 先取 ChatModelResponse
                ChatResponse response = respCtx.chatResponse();
                // 3. 再取 AiMessage
                AiMessage aiMessage = response.aiMessage();

                // 4. 工具调用
                List<ToolExecutionRequest> tools = aiMessage.toolExecutionRequests();
                for (ToolExecutionRequest t : tools) {
                    System.out.println("← tool      : " + t.name());
                    System.out.println("← arguments : " + t.arguments()); // 原始 JSON
                }

                // 5. 纯文本
                if (aiMessage.text() != null) {
                    System.out.println("← text      : " + aiMessage.text());
                }
            }

            @Override
            public void onError(ChatModelErrorContext errorCtx) {
                errorCtx.error().printStackTrace();
            }
        };

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .listeners(List.of(logger))
                .build();
    }

    @Value("${weather.tools.url}")
    private String weatherToolsUrl;

    @Value("${weather.tools.id}")
    private String weatherToolsId;

    @Value("${weather.tools.key}")
    private String weatherToolsKey;

    @Bean
    public WeatherTools weatherTools() {
        WeatherTools tools = new WeatherTools();
        tools.setWeatherToolsUrl(weatherToolsUrl);
        tools.setWeatherToolsId(weatherToolsId);
        tools.setWeatherToolsKey(weatherToolsKey);
        return tools;
    }

    @Bean
    public Assistant assistant(OpenAiChatModel chatModel, WeatherTools weatherTools) {
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .tools(weatherTools)
                .build();
    }
}