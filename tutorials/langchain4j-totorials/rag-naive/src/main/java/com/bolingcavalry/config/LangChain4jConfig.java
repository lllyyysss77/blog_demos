package com.bolingcavalry.config;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bolingcavalry.service.Assistant;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
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
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
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
        // DocumentParser的作用是把磁盘上的文件转为Document对象，以便后面的分块处理，
        // TextDocumentParser处理文本类文件，如txt、md等，如果要处理更多类型，可以用ApacheTikaDocumentParser，代价是包更大，启动更慢
        DocumentParser documentParser = new TextDocumentParser();
        long start = System.currentTimeMillis();
        logger.info("开始加载索引文件：{}", ragFilePath);
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(ragFilePath, documentParser);
        logger.info("加载索引文件完成，耗时: {}毫秒, 文件数量: {}",
                (System.currentTimeMillis() - start), documents.size());

        start = System.currentTimeMillis();    
        logger.info("开始对文档分块，共{}个文档", documents.size());    
        List<TextSegment> segments = new ArrayList<>();

        // 每个文档分块
        for (Document document : documents) {
            // 文档分块, 每个分块300个字符, 重叠0个字符
            DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
            segments.addAll(splitter.split(document));
        }
        logger.info("文档分块完成，共{}个分块，耗时: {}毫秒", segments.size(), (System.currentTimeMillis() - start));
        
        start = System.currentTimeMillis();    
        logger.info("开始将文档分块转为向量"); 
        // 每个分块创建嵌入向量
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);
        logger.info("文档分块转为向量完成，共{}个向量，耗时: {}秒", embeddings.size(), (System.currentTimeMillis() - start) / 1000);

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