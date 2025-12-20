package com.bolingcavalry.service;

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
    private Assistant assistant;

    /**
     * 最简单的高级API对话
     * 
     * @param prompt
     * @return
     */
    public String simpleChat(String prompt) {
        String answer = assistant.simpleChat(prompt);
        logger.info("响应：" + answer);
        return answer + "[from simpleChat with code and annotation]";
    }
}
