package com.bolingcavalry.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * 通义千问服务类，用于与通义千问模型进行交互
 */
@Service
public class QwenService {

    @Autowired
    private OpenAiChatModel modelFromSchema;

    @Autowired
    private OpenAiChatModel modelFromObject;


    /**
     * 调用通义千问模型进行对话
     * 
     * @param message 用户消息
     * @return AI回复
     */
    public String chatByModelFromSchema(String message) {
        return modelFromSchema.chat(message) + "from modelFromSchema";
    }

    /**
     * 调用通义千问模型进行对话
     * 
     * @param message 用户消息
     * @return AI回复
     */
    public String chatByModelFromObject(String message) {
        return modelFromObject.chat(message) + "from modelFromObject";
    }
}
