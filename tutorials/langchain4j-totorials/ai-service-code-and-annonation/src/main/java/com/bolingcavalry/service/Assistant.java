package com.bolingcavalry.service;

import dev.langchain4j.service.*;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface Assistant {
    /**
     * 最简单的对话，只返回助手的回答，不包含任何额外信息
     * 
     * @param userMessage 用户消息
     * @return 助手生成的回答
     */
    @SystemMessage("你是历史学家，回答问题是简洁风格，不超过100字")
    String simpleChat(String userMessage);
}