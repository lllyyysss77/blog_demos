package com.bolingcavalry.service;

public interface Assistant {
    /**
     * 通过提示词查询最新的天气情况
     * 
     * @param userMessage 用户消息
     * @return 助手生成的回答
     */
    String getWeather(String userMessage);
}