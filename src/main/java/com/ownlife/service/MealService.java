package com.ownlife.service;

import com.ownlife.entity.Food;
import com.ownlife.entity.MealLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface MealService {

    List<Food> getAllFoods();

    List<MealLog> getMealLogsByDate(Long memberId, LocalDate date);

    Map<String, Double> getDailySummary(Long memberId, LocalDate date);

    Map<String, Double> getDietGoalSummary(Long memberId, LocalDate date);

    Map<String, Object> getWeeklyIntakeSummary(Long memberId, LocalDate date);

    void addMeal(Long memberId, LocalDate date, String mealType, Long foodId, int count);

    void deleteMeal(Long memberId, Long mealLogId);

    void addCustomMeal(Long memberId, LocalDate date, String mealType, String customFoodName,
                       Double customBaseAmountG, int count, Double customCaloriesKcal, Double customCarbG,
                       Double customProteinG, Double customFatG, boolean saveAsFood);

}

