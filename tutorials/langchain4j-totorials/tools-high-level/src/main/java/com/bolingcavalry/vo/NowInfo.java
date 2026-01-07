package com.bolingcavalry.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class NowInfo implements Serializable {
    private double precipitation;
    private double temperature;
    private int pressure;
    private int humidity;
    private String windDirection;
    private int windDirectionDegree;
    private int windSpeed;
    private String windScale;
    private double feelst;
    private String uptime;
}