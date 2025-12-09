package com.bolingcavalry.service;

import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 通义千问服务类，用于与通义千问模型进行交互
 */
@Service
public class QwenService {

    private static final Logger logger = LoggerFactory.getLogger(QwenService.class);

    @Autowired
    private OpenAiChatModel openAiChatModel;

    @Autowired
    private Assistant assistant;

    /**
     * 低级API，手动添加原始聊天消息，实现聊天记忆功能
     * 
     * @param prompt 模板中的变量
     * @return 助手生成的回答
     */
    public String lowLevelAddRawChatMessage(String prompt) {
        // 这是第一次对话的请求
        UserMessage firstReq = UserMessage.from("一百字介绍曹操是谁");
        // 这是第一次对话的响应
        AiMessage firstResp = openAiChatModel.chat(firstReq).aiMessage();

        logger.info("第一次响应：" + firstResp.text());

        // 把第一次的请求响应，以及第二次的请求都放入集合，一共三条记录
        List<ChatMessage> history = List.of(firstReq, firstResp, UserMessage.from(prompt));

        // 这是第二次对话，已经带上了第一次的请求和响应
        AiMessage secondResp = openAiChatModel.chat(history).aiMessage();

        logger.info("第二次响应：" + secondResp.text());

        return secondResp.text() + "[from lowLevelAddRawChatMessage]";
    }

    /**
     * 创建一个ChatMemory实例，用于存储聊天记忆
     * 
     * @return ChatMemory实例
     */
    private ChatMemory createChatMemoryInstance() {
        // 设置记忆长度是基于token的，所以这里要根据模型名称设定分词方式
        String modelNameForToken = dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O.toString();
        // 可以基于最大token数量来创建，也可以基于最大消息数量来创建，方法是:MessageWindowChatMemory.withMaxMessages(100)
        return TokenWindowChatMemory.withMaxTokens(5000, new OpenAiTokenCountEstimator(modelNameForToken));
    }

    /**
     * 低级API，手动添加ChatMessage到ChatMemory，实现聊天记忆功能
     * 
     * @param prompt 模板中的变量
     * @return 助手生成的回答
     */
    public String lowLevelAddChatMessageToChatMemory(String prompt) {
        // 创建一个ChatMemory实例，通过token数量限制记忆长度
        ChatMemory chatMemory = createChatMemoryInstance();

        // 这是第一次对话
        // 把第一次的请求添加到ChatMemory中
        chatMemory.add(UserMessage.from("一百字介绍曹操是谁"));
        // 聊天，获取第一次的响应
        AiMessage firstAnswer = openAiChatModel.chat(chatMemory.messages()).aiMessage();

        logger.info("第一次响应：" + firstAnswer.text());
        // 把第一次的响应添加到ChatMemory中
        chatMemory.add(firstAnswer);

        // 这是第二次对话
        // 把第二次的请求添加到ChatMemory中
        chatMemory.add(UserMessage.from(prompt));
        // 聊天，获取第二次的响应
        AiMessage secondAnswer = openAiChatModel.chat(chatMemory.messages()).aiMessage();

        logger.info("第二次响应：" + secondAnswer.text());
        return secondAnswer.text() + "[from lowLevelAddChatMessageToChatMemory]";
    }

    /**
     * 低级API，使用ConversationChain来实现聊天记忆功能
     * 
     * @param prompt 模板中的变量
     * @return 大模型生成的回答
     */
    public String lowLevelByConversationChain(String prompt) {
        // 创建一个ChatMemory实例，通过token数量限制记忆长度
        ChatMemory chatMemory = createChatMemoryInstance();

        // 创建一个ConversationChain实例来负责多轮聊天，并且把ChatMemory实例传入用于处理聊天记忆
        ConversationalChain chain = ConversationalChain.builder()
                .chatModel(openAiChatModel)
                .chatMemory(chatMemory)
                .build();

        // 通过chain进行对话，这是第一次问答
        String firstAnswer = chain.execute("一百字介绍曹操是谁");
        logger.info("第一次响应：" + firstAnswer);

        // 通过chain进行对话，这是第二次问答
        String secondAnswer = chain.execute(prompt);
        logger.info("第二次响应：" + secondAnswer);

        return secondAnswer + "[from lowLevelByConversationChain]";
    }

}
