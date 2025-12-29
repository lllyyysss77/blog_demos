package com.bolingcavalry.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bolingcavalry.vo.HistoryEvent;

/**
 * 通义千问服务类，用于与通义千问模型进行交互
 */
@Service
public class QwenService {

    @Autowired
    private Assistant assistantWithModelFromSchema;

    @Autowired
    private Assistant assistantWithModelFromObject;

    /**
     * 调用通义千问模型进行对话
     * 
     * @param message 用户消息
     * @return AI回复
     */
    public HistoryEvent chatByModelFromSchema(String message) {
        return assistantWithModelFromSchema.simpleChat(message);
    }

    /**
     * 调用通义千问模型进行对话
     * 
     * @param message 用户消息
     * @return AI回复
     */
    public HistoryEvent chatByModelFromObject(String message) {
        return assistantWithModelFromObject.simpleChat(message);
    }
}
