/*
 * @Author: 程序员欣宸 zq2599@gmail.com
 * @Date: 2025-11-28 09:41:33
 * @LastEditors: 程序员欣宸 zq2599@gmail.com
 * @LastEditTime: 2025-11-28 10:56:42
 * @FilePath: /langchain4j-totorials/demo-with-spring-boot/src/main/java/com/bolingcavalry/config/LangChain4jConfig.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package com.bolingcavalry.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

/**
 * LangChain4j配置类
 */
@Configuration
public class LangChain4jConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name:qwen3-max}")
    private String modelName;

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String baseUrl;

    /**
     * 创建并配置StreamingChatModel实例（使用通义千问的OpenAI兼容接口）
     * 
     * @return StreamingChatModel实例
     */
    @Bean
    public StreamingChatModel streamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .build();
    }

}