package com.bolingcavalry.service;

import com.bolingcavalry.vo.HistoryEvent;

public interface Assistant {
    /**
     * 最简单的对话，只返回助手的回答，不包含任何额外信息
     * 
     * @param userMessage 用户消息
     * @return 助手生成的回答
     */
    HistoryEvent simpleChat(String userMessage);
}