package com.bolingcavalry.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;

import lombok.Data;

@Data
public class HistoryEvent {
    @JsonProperty("main_characters")
    private List<String> mainCharacters;

    @JsonProperty("year")
    private String year;
    
    @JsonProperty("description")
    private String description;
    
    // 创建静态的ObjectMapper实例，避免重复创建
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 将JSON字符串反序列化为HistoryEvent对象
     * 
     * @param json JSON字符串
     * @return HistoryEvent对象
     * @throws IOException 如果JSON处理或映射失败
     */
    public static HistoryEvent fromJson(String json) throws IOException {
        return objectMapper.readValue(json, HistoryEvent.class);
    }
}
