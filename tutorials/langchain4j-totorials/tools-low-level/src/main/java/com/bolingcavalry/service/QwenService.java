package com.bolingcavalry.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bolingcavalry.vo.WeatherInfo;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * 通义千问服务类，用于与通义千问模型进行交互
 */
@Service
public class QwenService {

    private static final Logger logger = LoggerFactory.getLogger(QwenService.class);

    @Autowired
    private OpenAiChatModel openAiChatModel;

    @Autowired
    private WeatherTools weatherTools;

    private List<ToolSpecification> prepareToolSpecifications() {
        return ToolSpecifications.toolSpecificationsFrom(WeatherTools.class);
    }

    /**
     * 处理工具执行请求
     */
    private String executeTool(ToolExecutionRequest request) {
        try {
            if ("getWeather".equals(request.name())) {
                String arguments = request.arguments();
                logger.info("执行工具调用：getWeather，参数：{}", arguments);
                
                // 简单解析 JSON 参数
                String province = null;
                String city = null;
                
                // 检查参数格式
                if (arguments.contains("arg0") && arguments.contains("arg1")) {
                    // 格式：{"arg0": "广东", "arg1": "深圳"}
                    province = extractValue(arguments, "arg0");
                    city = extractValue(arguments, "arg1");
                } else if (arguments.contains("province") && arguments.contains("city")) {
                    // 格式：{"province": "广东", "city": "深圳"}
                    province = extractValue(arguments, "province");
                    city = extractValue(arguments, "city");
                }
                
                logger.info("解析后的参数：province={}, city={}", province, city);
                
                if (province == null || city == null) {
                    throw new IllegalArgumentException("无法解析参数：" + arguments);
                }
                
                WeatherInfo weatherInfo = weatherTools.getWeather(province, city);
                return weatherInfo.toString();
            } else {
                return "Unknown tool: " + request.name();
            }
        } catch (Exception e) {
            logger.error("工具执行失败", e);
            return "Tool execution failed: " + e.getMessage();
        }
    }
    
    /**
     * 从 JSON 字符串中提取值
     */
    private String extractValue(String json, String key) {
        int start = json.indexOf('"' + key + '"');
        if (start == -1) return null;
        
        int colon = json.indexOf(':', start);
        int valueStart = json.indexOf('"', colon);
        int valueEnd = json.indexOf('"', valueStart + 1);
        
        return valueStart != -1 && valueEnd != -1 ? json.substring(valueStart + 1, valueEnd) : null;
    }
    
    /**
     * 通过提示词range大模型返回JSON格式的内容
     * 
     * @param prompt
     * @return
     */
    public String getWeather(String prompt) {
        List<ToolSpecification> toolSpecifications = prepareToolSpecifications();

        ChatRequest req = ChatRequest.builder()
                .messages(UserMessage.from(prompt))
                .toolSpecifications(toolSpecifications)
                .build();

        ChatResponse resp = openAiChatModel.chat(req);
        
        logger.info("初始响应：" + resp);

        // 检查是否需要执行工具调用
        if (resp.aiMessage().toolExecutionRequests() != null && !resp.aiMessage().toolExecutionRequests().isEmpty()) {
            logger.info("需要执行工具调用");
            
            // 执行所有工具调用
            for (ToolExecutionRequest toolRequest : resp.aiMessage().toolExecutionRequests()) {
                String toolResult = executeTool(toolRequest);

                logger.info("工具执行结果：" + toolResult);
                
                // 将工具执行结果发送回模型
                    ChatRequest toolResultRequest = ChatRequest.builder()
                            .messages(
                                    resp.aiMessage(),
                                    ToolExecutionResultMessage.from(toolRequest, "工具执行结果：" + toolResult)
                            )
                            .toolSpecifications(toolSpecifications)
                            .build();
                
                resp = openAiChatModel.chat(toolResultRequest);
                logger.info("工具执行后的响应：" + resp);
            }
        }

        return resp.aiMessage().text() + "[from getWeather]";
    }
}
