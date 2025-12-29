package com.bolingcavalry.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bolingcavalry.service.Assistant;

import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
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

        @Bean("modelFromSchema")
        public OpenAiChatModel modelFromSchema() {
                JsonSchema jsonSchema = JsonSchema.builder()
                                .name("HistoryEvent") // OpenAI 要求顶层 schema 有名字
                                .rootElement(
                                                JsonObjectSchema.builder()
                                                                .addProperty("mainCharacters", // 字符串数组
                                                                                JsonArraySchema.builder()
                                                                                                .items(new JsonStringSchema())
                                                                                                .build())
                                                                .addProperty("year", new JsonIntegerSchema())
                                                                .addProperty("description", new JsonStringSchema())
                                                                .required("mainCharacters", "year", "description")
                                                                .build())
                                .build();

                return OpenAiChatModel.builder()
                                .apiKey(apiKey)
                                .modelName(modelName)
                                .baseUrl(baseUrl)
                                .responseFormat(ResponseFormat.builder()
                                                .type(ResponseFormatType.JSON)
                                                .jsonSchema(jsonSchema)
                                                .build())
                                .build();
        }

        @Bean("modelWithJSONFormat")
        public OpenAiChatModel modelFromObject() {
                return OpenAiChatModel.builder()
                                .apiKey(apiKey)
                                .modelName(modelName)
                                .baseUrl(baseUrl)
                                .responseFormat(ResponseFormat.JSON)
                                .build();
        }

        @Bean
        public Assistant assistantWithModelFromSchema(@Qualifier("modelFromSchema") OpenAiChatModel modelFromSchema) {
                return AiServices.create(Assistant.class, modelFromSchema);
        }

        @Bean
        public Assistant assistantWithModelFromObject(@Qualifier("modelWithJSONFormat") OpenAiChatModel modelWithJSONFormat) {
                return AiServices.create(Assistant.class, modelWithJSONFormat);
        }
}