package com.bolingcavalry.util;

import java.util.Map;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import com.bolingcavalry.persistence.EmbeddedByIdDb;
import com.bolingcavalry.persistence.EmbeddedGlobalDb;

import static org.mapdb.Serializer.*;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

public class Tools {
    /**
     * 创建一个ChatMemory实例，用于存储聊天记忆，存在内存中
     * 
     * @return ChatMemory实例
     */
    public static ChatMemory createRamChatMemoryInstance() {
        // 设置记忆长度是基于token的，所以这里要根据模型名称设定分词方式
        String modelNameForToken = dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O.toString();
        // 可以基于最大token数量来创建，也可以基于最大消息数量来创建，方法是:MessageWindowChatMemory.withMaxMessages(100)
        return TokenWindowChatMemory.withMaxTokens(5000, new OpenAiTokenCountEstimator(modelNameForToken));
    }

    /**
     * 创建一个ChatMemoryStore实例，用于存储聊天记忆，存在数据库中
     * 
     * @return ChatMemoryStore实例
     */
    public static ChatMemoryStore createStoreInstance(String dbName, boolean isById) {
        ChatMemoryStore rlt = null;
        // 创建一个MapDB实例，用于存储聊天记忆
        DB db = DBMaker.fileDB(dbName).transactionEnable().make();

        if (isById) {
            Map<Integer, String> dbMap = db.hashMap("messages", INTEGER, STRING).createOrOpen();
            rlt = new EmbeddedByIdDb(db, dbMap);
        } else {
            Map<String, String> dbMap = db.hashMap("messages", STRING, STRING).createOrOpen();
            rlt = new EmbeddedGlobalDb(db, dbMap);
        }

        return rlt;
    }

    /**
     * 创建一个ChatMemory实例，用于存储聊天记忆，存在数据库中
     * 
     * @return ChatMemory实例
     */
    public static ChatMemory createDbChatMemoryInstance(String dbName, boolean isById) {
        // 创建一个MapDB实例，用于存储聊天记忆
        ChatMemoryStore store = createStoreInstance(dbName, isById);

        return MessageWindowChatMemory
                .builder()
                .maxMessages(100)
                .chatMemoryStore(store)
                .build();
    }

}
