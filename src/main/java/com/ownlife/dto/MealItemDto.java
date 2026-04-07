package com.ownlife.dto;

import lombok.Data;

@Data
public class MealItemDto {
    private Long mealLogId;
    private String foodName;
    private double caloriesKcal;
    private double carbG;
    private double proteinG;
    private double fatG;
}