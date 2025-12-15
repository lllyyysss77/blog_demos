package com.bolingcavalry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 通义千问服务类，用于与通义千问模型进行交互
 */
@Service
public class QwenService {

    private static final Logger logger = LoggerFactory.getLogger(QwenService.class);

    @Autowired
    private StreamingChatModel streamingChatModel;

    /**
     * 普通流式聊天方法（仅记录日志）
     * 
     * @param prompt 提示词
     */
    public void normalStreamingChat(String prompt) {
        streamingChatModel.chat(prompt, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                logger.info("partialResponse: {}", partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                logger.info("completeResponse: {}", completeResponse);
            }

            @Override
            public void onError(Throwable throwable) {
                logger.error("throwable: {}", throwable);
            }
        });
    }

    /**
     * SSE流式聊天方法（用于网页实时显示）
     * 
     * @param prompt 提示词
     * @return SseEmitter实例，用于向客户端发送流式响应
     */
    public SseEmitter streamingChat(String prompt) {
        // 创建SseEmitter，设置超时时间为30分钟
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        // 使用streamingChatModel进行流式聊天
        streamingChatModel.chat(prompt, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                if (partialResponse == null || partialResponse.trim().isEmpty()) {
                    logger.warn("空的部分响应，忽略");
                    return;
                }

                try {
                    // 发送部分响应到客户端
                    emitter.send(partialResponse);
                    logger.info("partialResponse: {}", partialResponse);
                } catch (Exception e) {
                    logger.error("发送部分响应失败: {}", e.getMessage(), e);
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                try {
                    // 发送完成标志
                    emitter.send(SseEmitter.event().name("complete"));
                    // 完成响应
                    emitter.complete();
                    logger.info("completeResponse: {}", completeResponse);
                } catch (Exception e) {
                    logger.error("发送完成响应失败: {}", e.getMessage(), e);
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                logger.error("流式聊天发生错误: {}", throwable.getMessage(), throwable);
                emitter.completeWithError(throwable);
            }
        });

        // 设置超时处理
        emitter.onTimeout(() -> {
            logger.error("SSE连接超时");
            emitter.completeWithError(new RuntimeException("连接超时"));
        });

        // 设置完成处理
        emitter.onCompletion(() -> {
            logger.info("SSE连接完成");
        });

        return emitter;
    }

    /**
     * 高级API的流式响应接口定义
     */
    interface StreamingAssistant {
        @SystemMessage("你是一个专业的AI助手，能够提供准确、清晰的回答。")
        TokenStream chat(@UserMessage String message);
    }

    // 高级API的AI服务实例（延迟初始化）
    private StreamingAssistant streamingAssistant;

    /**
     * Spring bean初始化完成后执行
     */
    @PostConstruct
    public void init() {
        // 在依赖注入完成后创建高级API的AI服务实例
        this.streamingAssistant = AiServices.builder(StreamingAssistant.class)
                .streamingChatModel(streamingChatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    /**
     * 基于高级API的SSE流式聊天方法
     * 
     * @param prompt 提示词
     * @return SseEmitter实例，用于向客户端发送流式响应
     */
    public SseEmitter highLevelStreamingChat(String prompt) {
        // 创建SseEmitter，设置超时时间为30分钟
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        try {
            // 使用高级API的TokenStream
            TokenStream tokenStream = streamingAssistant.chat(prompt);

            // 注册回调函数
            tokenStream.onPartialResponse(token -> {
                if (token == null || token.trim().isEmpty()) {
                    logger.warn("空的token，忽略");
                    return;
                }

                try {
                    // 发送token到客户端
                    emitter.send(token);
                    logger.info("token: {}", token);
                } catch (Exception e) {
                    logger.error("发送token失败: {}", e.getMessage(), e);
                    emitter.completeWithError(e);
                }
            });

            tokenStream.onError(throwable -> {
                logger.error("高级API流式聊天发生错误: {}", throwable.getMessage(), throwable);
                emitter.completeWithError(throwable);
            });

            tokenStream.onCompleteResponse(completeResponse -> {
                try {
                    // 发送完成标志
                    emitter.send(SseEmitter.event().name("complete"));
                    // 完成响应
                    emitter.complete();
                    logger.info("高级API流式聊天完成，完整响应: {}", completeResponse);
                } catch (Exception e) {
                    logger.error("发送完成响应失败: {}", e.getMessage(), e);
                    emitter.completeWithError(e);
                }
            });

            // 启动流处理
            tokenStream.start();
        } catch (Exception e) {
            logger.error("创建高级API流式响应失败: {}", e.getMessage(), e);
            emitter.completeWithError(e);
        }

        // 设置超时处理
        emitter.onTimeout(() -> {
            logger.error("高级API SSE连接超时");
            emitter.completeWithError(new RuntimeException("连接超时"));
        });

        // 设置完成处理
        emitter.onCompletion(() -> {
            logger.info("高级API SSE连接完成");
        });

        return emitter;
    }

}
