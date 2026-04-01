package com.ownlife.service;

import com.ownlife.entity.Food;
import com.ownlife.entity.MealLog;
import com.ownlife.repository.FoodRepository;
import com.ownlife.repository.MealLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MealServiceImpl implements MealService {

    private final FoodRepository foodRepository;
    private final MealLogRepository mealLogRepository;

    /**
     * 전체 음식 조회
     */
    @Override
    public List<Food> getAllFoods() {
        return foodRepository.findAll();
    }

    /**
     * 날짜별 식단 조회
     */
    @Override
    public List<MealLog> getMealLogsByDate(Long memberId, LocalDate date) {
        return mealLogRepository.findByMemberIdAndMealDate(memberId, date);
    }

    /**
     * 하루 총합 계산
     */
    @Override
    public Map<String, Double> getDailySummary(Long memberId, LocalDate date) {

        List<MealLog> logs = mealLogRepository.findByMemberIdAndMealDate(memberId, date);

        double totalKcal = 0;
        double totalCarb = 0;
        double totalProtein = 0;
        double totalFat = 0;

        for (MealLog log : logs) {
            totalKcal += log.getCaloriesKcal();
            totalCarb += log.getCarbG();
            totalProtein += log.getProteinG();
            totalFat += log.getFatG();
        }

        Map<String, Double> summary = new HashMap<>();
        summary.put("kcal", totalKcal);
        summary.put("carb", totalCarb);
        summary.put("protein", totalProtein);
        summary.put("fat", totalFat);

        return summary;
    }

    /**
     * 식단 추가 (🔥 핵심 로직)
     */
    @Override
    public void addMeal(Long memberId, LocalDate date, String mealType, Long foodId, int count) {

        // 1. 음식 조회
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new IllegalArgumentException("음식 없음"));

        // 2. count → g 변환
        double intakeG = count * food.getBaseAmountG();

        // 3. 100g 기준 → 실제 섭취량 기준으로 계산
        double ratio = intakeG / 100.0;

        double kcal = food.getCaloriesKcal() * ratio;
        double carb = food.getCarbG() * ratio;
        double protein = food.getProteinG() * ratio;
        double fat = food.getFatG() * ratio;

        // 4. MealLog 생성 (스냅샷 포함)
        MealLog mealLog = new MealLog();
        mealLog.setMemberId(memberId);
        mealLog.setMealDate(date);
        mealLog.setMealType(mealType);

        mealLog.setFoodId(foodId);
        mealLog.setFoodNameSnapshot(food.getFoodName());

        mealLog.setIntakeG(intakeG);

        mealLog.setCaloriesKcal(kcal);
        mealLog.setCarbG(carb);
        mealLog.setProteinG(protein);
        mealLog.setFatG(fat);

        // 5. 저장
        mealLogRepository.save(mealLog);
    }

    /**
     * 식단 삭제
     */
    @Override
    public void deleteMeal(Long memberId, Long mealLogId) {

        MealLog log = mealLogRepository.findById(mealLogId)
                .orElseThrow(() -> new IllegalArgumentException("기록 없음"));

        // (선택) 본인 검증
        if (!log.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("삭제 권한 없음");
        }

        mealLogRepository.delete(log);
    }
}