package com.bolingcavalry.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bolingcavalry.vo.IndexConfig;
import com.clickhouse.data.ClickHouseDataType;

import dev.langchain4j.community.store.embedding.clickhouse.ClickHouseEmbeddingStore;
import dev.langchain4j.community.store.embedding.clickhouse.ClickHouseSettings;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15q.BgeSmallZhV15QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

/**
 * 通义千问服务类，用于与通义千问模型进行交互
 */
@Service
public class QwenService {

    private static final Logger logger = LoggerFactory.getLogger(QwenService.class);

    @Autowired
    private Assistant assistant;

    @Autowired
    private IndexConfig clickHouseConfig;

    /**
     * 通过提示词range大模型返回JSON格式的内容
     * 
     * @param prompt
     * @return
     */
    public String byRagNaive(String prompt) {
        String answer = assistant.byRagNaive(prompt);
        logger.info("响应：" + answer);
        return answer + "[from byRagNaive]";
    }

    private void doIndex() {
        // 每个分块创建嵌入向量，模型是智源 bge-small-zh-v1.5 量化版，中文 C-MTEB 第一梯队
        EmbeddingModel embeddingModel = new BgeSmallZhV15QuantizedEmbeddingModel();
            // 将元数据键映射到 ClickHouse 数据类型
            Map<String, ClickHouseDataType> metadataTypeMap = new HashMap<>();

            logger.info("ClickHouse配置：{}", clickHouseConfig);
            
            ClickHouseSettings clickHouseSettings = ClickHouseSettings.builder()
                    .url(clickHouseConfig.getCkURL())
                    .table(clickHouseConfig.getCkTableName())
                    .username(clickHouseConfig.getCkUsername())
                    .password(clickHouseConfig.getCkPassword())
                    .dimension(embeddingModel.dimension())
                    .metadataTypeMap(metadataTypeMap)
                    .build();

        // DocumentParser的作用是把磁盘上的文件转为Document对象，以便后面的分块处理，
        // TextDocumentParser处理文本类文件，如txt、md等，如果要处理更多类型，可以用ApacheTikaDocumentParser，代价是包更大，启动更慢
        DocumentParser documentParser = new TextDocumentParser();
        long start = System.currentTimeMillis();
        logger.info("开始加载索引文件：{}", clickHouseConfig.getRagFilePath());
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(clickHouseConfig.getRagFilePath(), documentParser);
        logger.info("加载索引文件完成，耗时: {}毫秒, 文件数量: {}",
                (System.currentTimeMillis() - start), documents.size());

        start = System.currentTimeMillis();
        logger.info("开始对文档分块，共{}个文档", documents.size());
        List<TextSegment> segments = new ArrayList<>();

        // 每个文档分块
        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            try {
                logger.info("处理第{}个文档", i + 1);
                // 文档分块, 每个分块300个字符, 重叠0个字符
                try {
                    // 尝试使用递归分割器
                    DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
                    segments.addAll(splitter.split(document));
                    logger.info("第{}个文档加载完成", i + 1);
                } catch (RuntimeException e) {
                    logger.error("处理第{}个文档时出错: {}", i + 1, e.getMessage());
                    // 如果递归分割器失败（比如遇到特殊字符），使用手动分割
                    if (e.getMessage().contains("doesn't fit into the maximum segment size")) {
                        logger.warn("递归分割器失败，使用手动分割: {}", e.getMessage());
                        // 手动分割文档
                        String text = document.text();
                        int maxSegmentSize = 300;
                        for (int j = 0; j < text.length(); j += maxSegmentSize) {
                            int end = Math.min(j + maxSegmentSize, text.length());
                            String segmentText = text.substring(j, end);
                            // 检查segmentText是否为空或空白
                            if (segmentText != null && !segmentText.trim().isEmpty()) {
                                TextSegment segment = TextSegment.from(segmentText, document.metadata());
                                segments.add(segment);
                            }
                        }
                    } else {
                        // 其他错误，直接抛出
                        throw e;
                    }
                }
            } catch (Exception e) {
                logger.error("处理文档时出错: {}", e.getMessage());
                logger.error("文档内容长度: {}", document.text().length());
                // 如果文档内容很长，只打印前100个字符
                if (document.text().length() > 100) {
                    logger.error("文档前100个字符: {}", document.text().substring(0, 100));
                } else {
                    logger.error("文档内容: {}", document.text());
                }
                throw e;
            }
        }
        logger.info("文档分块完成，共{}个分块，耗时: {}毫秒", segments.size(), (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        logger.info("开始将文档分块转为向量");
        
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        ClickHouseEmbeddingStore embeddingStore = ClickHouseEmbeddingStore.builder()
                .settings(clickHouseSettings)
                .build();
        
        embeddingStore.addAll(embeddings, segments);


        logger.info("文档分块转为向量完成，共{}个向量，耗时: {}秒", embeddings.size(), (System.currentTimeMillis() - start) / 1000);
    }

    /**
     * 开始索引
     * 
     * @return
     */
    public String startIndex() {
        // 在新的线程中操作，避免阻塞主线程
        new Thread(() -> {
            doIndex();
        }).start();

        return "已开始索引，请通过后台日志确认索引工作进展";
    }
}
