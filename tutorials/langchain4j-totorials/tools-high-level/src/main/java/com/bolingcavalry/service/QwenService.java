package com.bolingcavalry.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 通义千问服务类，用于与通义千问模型进行交互
 */
@Service
public class QwenService {

    @Autowired
    private Assistant assistant;

    /**
     * 通过提示词range大模型返回JSON格式的内容
     * 
     * @param prompt
     * @return
     */
    public String getWeather(String prompt) {
        return assistant.getWeather(prompt)+ "[from high level getWeather]";
    }
}
