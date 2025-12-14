/*
 * @Author: 程序员欣宸 zq2599@gmail.com
 * @Date: 2025-11-28 09:41:52
 * @LastEditors: 程序员欣宸 zq2599@gmail.com
 * @LastEditTime: 2025-11-28 11:37:52
 * @FilePath: /langchain4j-totorials/demo-with-spring-boot/src/main/java/com/bolingcavalry/controller/QwenController.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package com.bolingcavalry.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
     * 检查请求体是否有效
     * 
     * @param request 包含提示词的请求体
     * @return 如果有效则返回null，否则返回包含错误信息的ResponseEntity
     */
    private ResponseEntity<Response> check(PromptRequest request) {
        if (request == null || request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new Response("提示词不能为空"));
        }
        return null;
    }

    /**
     * 检查提示词参数是否有效
     * 
     * @param prompt 提示词参数
     * @return 如果有效则返回null，否则返回包含错误信息的ResponseEntity
     */
    private ResponseEntity<String> checkPrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("提示词不能为空");
        }
        return null;
    }

    /**
     * 普通流式聊天接口
     * 
     * @param request 包含提示词的请求体
     * @return 包含模型响应的ResponseEntity
     */
    @PostMapping("/normalstreamingchat")
    public ResponseEntity<Response> normalStreamingChat(@RequestBody PromptRequest request) {
        ResponseEntity<Response> checkRlt = check(request);
        if (checkRlt != null) {
            return checkRlt;
        }

        try {
            // 调用QwenService进行流式聊天
            qwenService.normalStreamingChat(request.getPrompt());
            return ResponseEntity.ok(new Response("流式聊天开始"));
        } catch (Exception e) {
            // 捕获异常并返回错误信息
            return ResponseEntity.status(500).body(new Response("流式聊天失败: " + e.getMessage()));
        }
    }

    /**
     * SSE流式聊天接口（用于网页实时显示）
     * 
     * @param prompt 提示词
     * @param userId 用户ID
     * @return SseEmitter实例，用于向客户端发送流式响应
     */
    @GetMapping("/sse-streaming-chat")
    public SseEmitter sseStreamingChat(@RequestParam(name = "prompt") String prompt,
            @RequestParam(name = "userId") int userId) {
        // 检查提示词是否有效
        ResponseEntity<String> checkRlt = checkPrompt(prompt);
        if (checkRlt != null) {
            // 如果提示词无效，创建一个错误的SseEmitter
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new IllegalArgumentException("提示词不能为空"));
            return emitter;
        }

        try {
            // 调用QwenService的SSE流式聊天方法
            return qwenService.sseStreamingChat(prompt);
        } catch (Exception e) {
            // 捕获异常并返回错误的SseEmitter
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(e);
            return emitter;
        }
    }
}
