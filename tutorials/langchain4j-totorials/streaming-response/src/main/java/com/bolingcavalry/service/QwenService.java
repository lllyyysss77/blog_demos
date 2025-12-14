package com.bolingcavalry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

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
    public SseEmitter sseStreamingChat(String prompt) {
        // 创建SseEmitter，设置超时时间为30分钟
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        // 使用streamingChatModel进行流式聊天
        streamingChatModel.chat(prompt, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
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

}
