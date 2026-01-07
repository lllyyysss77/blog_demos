package com.bolingcavalry.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import com.bolingcavalry.vo.WeatherInfo;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.Data;

@Data
public class WeatherTools {

    private static final Logger logger = LoggerFactory.getLogger(WeatherTools.class);

    private String weatherToolsUrl;
    private String weatherToolsId;
    private String weatherToolsKey;


    @SuppressWarnings("null")
    @Tool("返回给定省份和城市的天气预报综合信息")
    public WeatherInfo getWeather(@P("应返回天气预报的省份") String province, @P("应返回天气预报的城市") String city) throws IllegalArgumentException {
        String encodedProvince = URLEncoder.encode(province, StandardCharsets.UTF_8);
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String url = String.format(weatherToolsUrl, weatherToolsId, weatherToolsKey, encodedProvince, encodedCity);
        logger.info("调用天气接口：{}", url);
        return new RestTemplate().getForObject(url, WeatherInfo.class);
    }
}
