package com.ownlife.service;

import com.ownlife.dto.DietPageDataDto;
import com.ownlife.entity.Food;
import com.ownlife.entity.MealLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface MealService {

    DietPageDataDto getDietPageData(Long memberId, LocalDate date);

    List<Food> getAllFoods();

    List<MealLog> getMealLogsByDate(Long memberId, LocalDate date);

    Map<String, Double> getDailySummary(Long memberId, LocalDate date);

    Map<String, Double> getDietGoalSummary(Long memberId, LocalDate date);

    Map<String, Object> getWeeklyIntakeSummary(Long memberId, LocalDate date);

    Map<String, Object> getDietDayChart(Long memberId, LocalDate date);

    Map<String, Object> getDietWeekChart(Long memberId, LocalDate date);

    Map<String, Object> getDietMonthChart(Long memberId, LocalDate date);

    Map<String, Object> getDietYearChart(Long memberId, LocalDate date);

    void addMeal(Long memberId, LocalDate date, String mealType, Long foodId, double count);

    void deleteMeal(Long memberId, Long mealLogId);

    void addMeals(Long memberId, LocalDate date, String mealType, String selectedFoodsJson);

    void addCustomMeal(Long memberId, LocalDate date, String mealType, String customFoodName,
                       Double customBaseAmountG, double count, Double customCaloriesKcal, Double customCarbG,
                       Double customProteinG, Double customFatG, boolean saveAsFood);

    void deleteMealGroup(Long memberId, LocalDate date, String mealType);

}

