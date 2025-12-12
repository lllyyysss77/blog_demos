package com.bolingcavalry.service;

import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bolingcavalry.util.Tools;

/**
 * 通义千问服务类，用于与通义千问模型进行交互
 */
@Service
public class QwenService {

    private static final String DESC_HUMAN = "一百字介绍曹操是谁";

    private static final Logger logger = LoggerFactory.getLogger(QwenService.class);

    @Autowired
    private OpenAiChatModel openAiChatModel;

    @Autowired
    private Assistant assistantRamGlobal;

    @Autowired
    private Assistant assistantRamById;

    @Autowired
    private Assistant assistantDbGlobal;

    @Autowired
    private Assistant assistantDbById;

    private List<ChatMessage> history = new ArrayList<>();

    private ChatMemory chatMemory = null;

    private ConversationalChain chain = null;

    /**
     * 1. 低级API，手动添加原始聊天消息，实现聊天记忆功能
     * 
     * @param prompt 模板中的变量
     * @return 助手生成的回答
     */
    public String lowLevelAddRawChatMessage(String prompt) {
        // 每一次的请求都存入历史对象
        history.add(UserMessage.from(prompt));

        // 对话
        AiMessage resp = openAiChatModel.chat(history).aiMessage();

        // 每一次的响应都存入历史对象
        history.add(resp);

        logger.info("响应：" + resp.text());

        return resp.text() + "[from lowLevelAddRawChatMessage]";
    }

    /**
     * 2. 低级API，手动添加ChatMessage到ChatMemory，实现聊天记忆功能
     * 
     * @param prompt 模板中的变量
     * @return 助手生成的回答
     */
    public String lowLevelAddChatMessageToChatMemory(String prompt) {
        // 创建一个ChatMemory实例，通过token数量限制记忆长度
        if (null == chatMemory) {
            chatMemory = Tools.createRamChatMemoryInstance();
        }

        // 每一次的请求都存入添加到ChatMemory中
        chatMemory.add(UserMessage.from(prompt));
        // 聊天
        AiMessage answer = openAiChatModel.chat(chatMemory.messages()).aiMessage();
        // 每一次的响应都存入添加到ChatMemory中
        chatMemory.add(answer);

        logger.info("响应：" + answer.text());
        return answer.text() + "[from lowLevelAddChatMessageToChatMemory]";
    }

    /**
     * 3. 低级API，使用ConversationChain来实现聊天记忆功能
     * 
     * @param prompt 模板中的变量
     * @return 大模型生成的回答
     */
    public String lowLevelByConversationChain(String prompt) {
        // 创建一个ChatMemory实例，通过token数量限制记忆长度
        ChatMemory chatMemory = Tools.createRamChatMemoryInstance();

        // 创建一个ConversationChain实例来负责多轮聊天，并且把ChatMemory实例传入用于处理聊天记忆
        if (null == chain) {
            chain = ConversationalChain.builder()
                    .chatModel(openAiChatModel)
                    .chatMemory(chatMemory)
                    .build();
        }

        // 通过chain进行对话，这是第二次问答
        String answer = chain.execute(prompt);
        logger.info("第二次响应：" + answer);

        return answer + "[from lowLevelByConversationChain]";
    }

    /**
     * 4. 高级API，基于内存的全局记忆
     * 
     * @param prompt
     * @return
     */
    public String highLevelRamGlobal(String prompt) {
        String answer = assistantRamGlobal.simpleChat(prompt);
        logger.info("响应：" + answer);
        return answer + "[from highLevelRamGlobal]";
    }

    /**
     * 5. 高级API，基于内存的用户记忆
     * 
     * @param userID
     * @param prompt
     * @return
     */
    public String highLevelRamByUserID(int userID, String prompt) {
        String secondAnswer = assistantRamById.chatByMemoryId(userID, prompt);
        logger.info("响应：" + secondAnswer);
        return secondAnswer + "[from highLevelRamByUserID]";
    }

    /**
     * 6. 高级API，基于数据库的全局记忆
     * 
     * @param prompt
     * @return
     */
    public String highLevelDbGlobal(String prompt) {
        String secondAnswer = assistantDbGlobal.simpleChat(prompt);
        logger.info("响应：" + secondAnswer);
        return secondAnswer + "[from highLevelDbGlobal]";
    }

    /**
     * 7. 高级API，基于数据库的用户记忆
     * 
     * @param userID
     * @param prompt
     * @return
     */
    public String highLevelDbByUserID(int userID, String prompt) {
        String answer = assistantDbById.chatByMemoryId(userID, prompt);
        logger.info("响应：" + answer);
        return answer + "[from highLevelDbByUserID]";

    }
}
