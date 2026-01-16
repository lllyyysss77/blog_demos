package com.bolingcavalry.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bolingcavalry.service.Assistant;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @Bean
    public Assistant assistant() {
        ContentRetriever contentRetriever = createContentRetriever(ragFilePath);
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel())
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(contentRetriever)
                .build();
    }

    private static ContentRetriever createContentRetriever(String ragFilePath) {
        // 向量数据存储的服务类，这里用的是内存存储，实际场景中可以用数据库或其他存储
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        long start = System.currentTimeMillis();
        logger.info("开始加载索引文件：{}", ragFilePath);
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(ragFilePath);
        logger.info("加载索引文件完成，耗时: {}秒, 文件数量: {}",
                (System.currentTimeMillis() - start) / 1000, documents.size());

        logger.info("开始索引RAG文件: {}，共{}个文档", ragFilePath, documents.size());
        start = System.currentTimeMillis();

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingStore(embeddingStore)
                .build();

        int totalDocuments = documents.size();

        // 逐个处理文档，添加进度日志
        for (int i = 0; i < totalDocuments; i++) {
            Document doc = documents.get(i);

            if (i % 10 == 0) {
                logger.info("处理进度: {}/{}", i + 1, totalDocuments);
            }

            ingestor.ingest(doc);
            logger.info("文档索引完成: {}", doc);
        }

        long end = System.currentTimeMillis();
        logger.info("所有文档索引完成，总耗时：{}秒", (end - start) / 1000);

        return EmbeddingStoreContentRetriever.from(embeddingStore);
    }
}