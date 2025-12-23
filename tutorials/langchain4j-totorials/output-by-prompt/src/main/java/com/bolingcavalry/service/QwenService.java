package com.bolingcavalry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bolingcavalry.vo.HistoryEvent;

import java.io.IOException;

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
    public String byPrompt(String prompt) {
        String answer = assistant.byPrompt(prompt);
        logger.info("响应：" + answer);

        HistoryEvent historyEvent = null;
        // 用大模型返回的字符串直接反序列化成对象
        try {
            historyEvent = HistoryEvent.fromJson(answer);
            logger.info("反序列化后的对象：" + historyEvent);
        } catch (IOException e) {
            logger.error("反序列化失败", e);
        }


        return answer + "[from byPrompt]";
    }
}
