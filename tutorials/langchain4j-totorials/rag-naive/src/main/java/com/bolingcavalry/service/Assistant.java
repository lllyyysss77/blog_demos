package com.bolingcavalry.service;

public interface Assistant {
    /**
     * 通过提示词range大模型返回JSON格式的内容
     * 
     * @param userMessage 用户消息
     * @return 助手生成的回答
     */
    String byRagNaive(String userMessage);
}