package com.bolingcavalry.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bolingcavalry.service.QwenService;
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

/**
 * LangChain4j配置类
 */
@Configuration
public class LangChain4jConfig {

    private static final Logger logger = LoggerFactory.getLogger(QwenService.class);

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
        ChatModelListener listener = new ChatModelListener() {
            @Override
            public void onRequest(ChatModelRequestContext reqCtx) {
                // 1. 拿到 List<ChatMessage>
                List<ChatMessage> messages = reqCtx.chatRequest().messages();
                logger.info("发到LLM的请求: {}", messages);
                
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
                    logger.info("LLM响应, 执行函数[{}], 函数入参 : {}", t.name(), t.arguments());
                }

                // 5. 纯文本
                if (aiMessage.text() != null) {
                    logger.info("LLM响应, 纯文本 : {}", aiMessage.text());
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
                .listeners(List.of(listener))
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
}