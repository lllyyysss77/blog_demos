/*
 * @Author: 程序员欣宸 zq2599@gmail.com
 * @Date: 2025-11-28 09:41:52
 * @LastEditors: 程序员欣宸 zq2599@gmail.com
 * @LastEditTime: 2025-11-28 11:37:52
 * @FilePath: /langchain4j-totorials/demo-with-spring-boot/src/main/java/com/bolingcavalry/controller/QwenController.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package com.bolingcavalry.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.bolingcavalry.service.QwenService;

import lombok.Data;

/**
 * 通义千问控制器，处理与大模型交互的HTTP请求
 */
@RestController
@RequestMapping("/api/qwen")
public class QwenController {

    private static final Logger logger = LoggerFactory.getLogger(QwenController.class);

    private final QwenService qwenService;

    /**
     * 构造函数，通过依赖注入获取QwenService实例
     * 
     * @param qwenService QwenService实例
     */
    public QwenController(QwenService qwenService) {
        this.qwenService = qwenService;
    }

    /**
     * 提示词请求实体类
     */
    @Data
    static class PromptRequest {
        private String prompt;
        private int userId;
    }

    /**
     * 响应实体类
     */
    @Data
    static class Response {
        private String result;

        public Response(String result) {
            this.result = result;
        }
    }

    /**
     * 检查提示词参数是否有效
     * 
     * @param prompt 提示词参数
     * @return 如果有效则返回null，否则返回包含错误信息的SseEmitter
     */
    private SseEmitter checkPrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new IllegalArgumentException("提示词不能为空"));
            return emitter;
        }
        return null;
    }

    /**
     * SSE流式聊天接口（用于网页实时显示）
     * 
     * @param prompt 提示词
     * @param userId 用户ID
     * @return SseEmitter实例，用于向客户端发送流式响应
     */
    @GetMapping("/sse-streaming-chat")
    public SseEmitter streamingChat(@RequestParam(name = "prompt") String prompt,
            @RequestParam(name = "userId") int userId) {
        logger.info("收到来自用户[{}]的请求, 提示词: {}", userId, prompt);
        // 检查提示词是否有效
        SseEmitter checkRlt = checkPrompt(prompt);
        if (checkRlt != null) {
            return checkRlt;
        }

        try {
            // 调用QwenService的流式聊天方法
            return qwenService.streamingChat(prompt);
        } catch (Exception e) {
            // 捕获异常并返回错误的SseEmitter
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(e);
            return emitter;
        }
    }

    /**
     * 基于高级API的SSE流式聊天接口（用于网页实时显示）
     * 
     * @param prompt 提示词
     * @param userId 用户ID
     * @return SseEmitter实例，用于向客户端发送流式响应
     */
    @GetMapping("/high-level-sse-streaming-chat")
    public SseEmitter highLevelStreamingChat(@RequestParam(name = "prompt") String prompt,
            @RequestParam(name = "userId") int userId) {
        logger.info("收到来自用户[{}]的高级API请求, 提示词: {}", userId, prompt);
        // 检查提示词是否有效
        SseEmitter checkRlt = checkPrompt(prompt);
        if (checkRlt != null) {
            return checkRlt;
        }

        try {
            // 调用QwenService的高级API流式聊天方法
            return qwenService.highLevelStreamingChat(prompt);
        } catch (Exception e) {
            // 捕获异常并返回错误的SseEmitter
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(e);
            return emitter;
        }
    }
}
