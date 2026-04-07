package com.ownlife.dto;

import lombok.Data;

import java.util.List;


@Data
public class MealGroupDto {
    private String mealType;       // BREAKFAST
    private String mealTypeLabel;  // 아침
    private List<MealItemDto> items;
    private double subtotalKcal;
}