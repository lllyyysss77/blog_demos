package com.bolingcavalry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bolingcavalry.vo.HistoryEvent;

/**
 * 通义千问服务类，用于与通义千问模型进行交互
 */
@Service
public class QwenService {

    private static final Logger logger = LoggerFactory.getLogger(QwenService.class);

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
        HistoryEvent rlt = assistantWithModelFromSchema.simpleChat(message);
        logger.info("2. 收到响应对象: {}", rlt);
        return rlt;
    }

    /**
     * 调用通义千问模型进行对话
     * 
     * @param message 用户消息
     * @return AI回复
     */
    public HistoryEvent chatByModelFromObject(String message) {
        HistoryEvent rlt = assistantWithModelFromObject.simpleChat(message);
        logger.info("1. 收到响应对象: {}", rlt);
        return rlt;
    }
}
