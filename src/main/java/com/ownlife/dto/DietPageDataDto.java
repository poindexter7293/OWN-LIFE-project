package com.ownlife.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class DietPageDataDto {
    private LocalDate selectedDate;
    private NutritionSummaryDto dailySummary;
    private GoalSummaryDto goalSummary;
    private DietChartBundleDto charts;
    private List<MealGroupDto> mealGroups;
}