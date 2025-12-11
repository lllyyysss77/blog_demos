package com.bolingcavalry.persistence;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.mapdb.DB;

import java.util.List;
import java.util.Map;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;

public class EmbeddedByIdDb implements ChatMemoryStore {

    private DB db;
    private Map<Integer, String> map;

    /**
     * 带Map参数的构造方法，使用用户提供的映射
     * 
     * @param map 用户提供的映射实例
     */
    public EmbeddedByIdDb(DB db, Map<Integer, String> map) {
        this.db = db;
        this.map = map;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = map.get((Integer) memoryId);
        return messagesFromJson(json);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String json = messagesToJson(messages);
        map.put((Integer) memoryId, json);
        // 只有当db不为null时才提交事务
        if (db != null) {
            db.commit();
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        map.remove((Integer) memoryId);
        // 只有当db不为null时才提交事务
        if (db != null) {
            db.commit();
        }
    }
}