package com.bolingcavalry.util;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;

public class Tools {
    /**
     * 创建一个ChatMemory实例，用于存储聊天记忆
     * 
     * @return ChatMemory实例
     */
    public static ChatMemory createChatMemoryInstance() {
        // 设置记忆长度是基于token的，所以这里要根据模型名称设定分词方式
        String modelNameForToken = dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O.toString();
        // 可以基于最大token数量来创建，也可以基于最大消息数量来创建，方法是:MessageWindowChatMemory.withMaxMessages(100)
        return TokenWindowChatMemory.withMaxTokens(5000, new OpenAiTokenCountEstimator(modelNameForToken));
    }
}
