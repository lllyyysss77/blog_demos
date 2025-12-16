package com.bolingcavalry.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * 流式助手接口，用于定义高级API的AI服务方法
 */
public interface StreamingAssistant {
    @SystemMessage("你是资深中国历史学者，回答问题的风格是清晰简洁")
    TokenStream chat(@UserMessage String message);
}