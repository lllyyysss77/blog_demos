package com.bolingcavalry.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/output/byfunctioncall")
    public ResponseEntity<Response> byPrompt(@RequestBody PromptRequest request) {
        ResponseEntity<Response> checkRlt = check(request);
        if (checkRlt != null) {
            return checkRlt;
        }

        try {
            String response = qwenService.byFunctionCall(request.getPrompt());
            return ResponseEntity.ok(new Response(response));
        } catch (Exception e) {
            // 捕获异常并返回错误信息
            return ResponseEntity.status(500).body(new Response("请求处理失败: " + e.getMessage()));
        }
    }
}
