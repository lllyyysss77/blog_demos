package com.bolingcavalry.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bolingcavalry.service.Assistant;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LangChain4j配置类
 */
@Configuration
public class LangChain4jConfig {

    private static final Logger logger = LoggerFactory.getLogger(LangChain4jConfig.class);

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name:qwen-turbo}")
    private String modelName;

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String baseUrl;

    @Value("${rag.file.path}")
    private String ragFilePath;

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
    public Assistant assistant() {
        ContentRetriever contentRetriever = createContentRetriever(ragFilePath);
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel()) // it should use OpenAI LLM
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10)) // it should remember 10 latest messages
                .contentRetriever(contentRetriever) // it should have access to our documents
                .build();
    }

    private static ContentRetriever createContentRetriever(String ragFilePath) {
        // 使用内存存储RAG文件内容
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        long start = System.currentTimeMillis();
        logger.info("开始加载索引文件：" + ragFilePath);
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(ragFilePath);
        logger.info("加载索引文件完成，耗时: " + (System.currentTimeMillis() - start) / 1000 + "秒, 文件数量: " + documents.size());

        // 开始索引
        logger.info("开始索引RAG文件: " + ragFilePath);
        start = System.currentTimeMillis();
        EmbeddingStoreIngestor.ingest(documents, embeddingStore);
        logger.info("索引完成，耗时：" + (System.currentTimeMillis() - start) / 1000 + "秒");

        return EmbeddingStoreContentRetriever.from(embeddingStore);
    }

}