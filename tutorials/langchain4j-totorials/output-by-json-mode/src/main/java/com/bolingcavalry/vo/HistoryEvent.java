package com.bolingcavalry.vo;

import java.util.List;

import lombok.Data;

@Data
public class HistoryEvent {
    private List<String> mainCharacters;
    private int year;
    private String description;
}
