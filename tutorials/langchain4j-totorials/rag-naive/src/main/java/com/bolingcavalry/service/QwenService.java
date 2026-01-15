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
     * 通过提示词range大模型返回JSON格式的内容
     * 
     * @param prompt
     * @return
     */
    public String byRagEasy(String prompt) {
        String answer = assistant.byRagEasy(prompt);
        logger.info("响应：" + answer);
        return answer + "[from byRagNaive]";
    }
}
