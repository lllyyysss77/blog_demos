package com.bolingcavalry.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class WeatherInfo implements Serializable {
    private int code;
    private String guo;
    private String sheng;
    private String shi;
    private String name;
    private String weather1;
    private String weather2;
    private int wd1;
    private int wd2;
    private String winddirection1;
    private String winddirection2;
    private String windleve1;
    private String windleve2;
    private String weather1img;
    private String weather2img;
    private double lon;
    private double lat;
    private String uptime;
    private NowInfo nowinfo;
    private Object alarm;
}