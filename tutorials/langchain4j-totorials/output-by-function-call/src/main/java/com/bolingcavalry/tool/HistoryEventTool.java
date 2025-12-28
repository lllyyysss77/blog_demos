package com.bolingcavalry.tool;

import java.util.List;

import org.springframework.stereotype.Component;

import com.bolingcavalry.vo.HistoryEvent;

import dev.langchain4j.agent.tool.Tool;

/**
 * 历史事件提取工具，用于从文本中提取历史事件信息
 */
@Component
public class HistoryEventTool {

    /**
     * 从文本中提取历史事件信息
     * 注意：LangChain4j会使用这个方法的签名来构建function call，而不是实际执行这个方法
     * 
     * @param mainCharacters 主要人物列表
     * @param year           发生年份
     * @param description    事件描述
     * @return 历史事件对象
     */
    @Tool("创建历史事件对象，包含主要人物、发生年份和事件描述")
    public HistoryEvent createHistoryEvent(List<String> mainCharacters, int year, String description) {
        return null;
    }
}
