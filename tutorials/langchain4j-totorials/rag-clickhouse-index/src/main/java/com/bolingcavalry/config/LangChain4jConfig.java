package com.bolingcavalry.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bolingcavalry.service.Assistant;
import com.bolingcavalry.vo.IndexConfig;
import com.clickhouse.data.ClickHouseDataType;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.community.store.embedding.clickhouse.ClickHouseEmbeddingStore;
import dev.langchain4j.community.store.embedding.clickhouse.ClickHouseSettings;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15q.BgeSmallZhV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

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

    @Value("${clickhouse.url}")
    private String clickhouseUrl;

    @Value("${clickhouse.table}")
    private String clickhouseTable;

    @Value("${clickhouse.username}")
    private String clickhouseUsername;

    @Value("${clickhouse.pswd}")
    private String clickhousePswd;

    @Bean
    public ClickHouseSettings clickHouseSettings() {
        EmbeddingModel embeddingModel = new BgeSmallZhV15QuantizedEmbeddingModel();

        // 将元数据键映射到 ClickHouse 数据类型
        Map<String, ClickHouseDataType> metadataTypeMap = new HashMap<>();

        return ClickHouseSettings.builder()
                .url(clickhouseUrl)
                .table(clickhouseTable)
                .username(clickhouseUsername)
                .password(clickhousePswd)
                .dimension(embeddingModel.dimension())
                .metadataTypeMap(metadataTypeMap)
                .build();
    }

    @Bean
    public IndexConfig clickHouseConfig() {
        IndexConfig config = new IndexConfig();
        config.setCkURL(clickhouseUrl);
        config.setCkTableName(clickhouseTable);
        config.setCkUsername(clickhouseUsername);
        config.setCkPassword(clickhousePswd);
        config.setRagFilePath(ragFilePath);
        return config;
    }

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
        // ContentRetriever contentRetriever = createContentRetriever(ragFilePath);
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel())
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                // .contentRetriever(contentRetriever)
                .build();
    }

    private ContentRetriever createContentRetriever(String ragFilePath) {

        // 每个分块创建嵌入向量，模型是智源 bge-small-zh-v1.5 量化版，中文 C-MTEB 第一梯队
        EmbeddingModel embeddingModel = new BgeSmallZhV15QuantizedEmbeddingModel();
        ClickHouseSettings settings = clickHouseSettings();
        ClickHouseEmbeddingStore embeddingStore = ClickHouseEmbeddingStore.builder()
                .settings(settings)
                .build();

        // 创建内容检索器
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2) // 每个查询返回2个最相关的分块
                .minScore(0.5) // 每个分块的相似度阈值
                .build();

        return contentRetriever;
    }
}