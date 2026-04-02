package com.ownlife.service;

import com.ownlife.entity.Food;
import com.ownlife.entity.MealLog;
import com.ownlife.repository.FoodRepository;
import com.ownlife.repository.MealLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ownlife.entity.Member;
import com.ownlife.repository.MemberRepository;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MealServiceImpl implements MealService {

    private final FoodRepository foodRepository;
    private final MealLogRepository mealLogRepository;
    private final MemberRepository memberRepository;

    /**
     전체 음식 조회
     */
    @Override
    public List<Food> getAllFoods() {
        return foodRepository.findAll();
    }

    /**
     날짜별 식단 조회
     */
    @Override
    public List<MealLog> getMealLogsByDate(Long memberId, LocalDate date) {
        return mealLogRepository.findByMemberIdAndMealDate(memberId, date);
    }

    /**
     하루 총합 계산
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
     식단 추가 (핵심 로직)
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
      식단 삭제
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

    /**
    식단 직접입력 메서드
     */
    @Override
    public void addCustomMeal(Long memberId,
                              LocalDate date,
                              String mealType,
                              String customFoodName,
                              Double customBaseAmountG,
                              int count,
                              Double customCaloriesKcal,
                              Double customCarbG,
                              Double customProteinG,
                              Double customFatG,
                              boolean saveAsFood) {

        if (customFoodName == null || customFoodName.isBlank()) {
            throw new IllegalArgumentException("음식명을 입력해 주세요.");
        }
        if (customBaseAmountG == null || customBaseAmountG <= 0) {
            throw new IllegalArgumentException("기준량(g)을 올바르게 입력해 주세요.");
        }
        if (customCaloriesKcal == null || customCaloriesKcal < 0) {
            throw new IllegalArgumentException("칼로리를 올바르게 입력해 주세요.");
        }
        if (customCarbG == null || customCarbG < 0) {
            throw new IllegalArgumentException("탄수화물을 올바르게 입력해 주세요.");
        }
        if (customProteinG == null || customProteinG < 0) {
            throw new IllegalArgumentException("단백질을 올바르게 입력해 주세요.");
        }
        if (customFatG == null || customFatG < 0) {
            throw new IllegalArgumentException("지방을 올바르게 입력해 주세요.");
        }
        if (saveAsFood) {
            Optional<Food> existingFood = foodRepository
                    .findByFoodNameAndCaloriesKcalAndCarbGAndProteinGAndFatGAndBaseAmountG(
                            customFoodName,
                            customCaloriesKcal,
                            customCarbG,
                            customProteinG,
                            customFatG,
                            customBaseAmountG
                    );

            if (existingFood.isEmpty()) {
                Food food = new Food();
                food.setFoodName(customFoodName);
                food.setCaloriesKcal(customCaloriesKcal);
                food.setCarbG(customCarbG);
                food.setProteinG(customProteinG);
                food.setFatG(customFatG);
                food.setBaseAmountG(customBaseAmountG);

                foodRepository.save(food);
            }
        }

        double intakeG = count * customBaseAmountG;
        double ratio = intakeG / 100.0;

        double kcal = customCaloriesKcal * ratio;
        double carb = customCarbG * ratio;
        double protein = customProteinG * ratio;
        double fat = customFatG * ratio;

        MealLog mealLog = new MealLog();
        mealLog.setMemberId(memberId);
        mealLog.setMealDate(date);
        mealLog.setMealType(mealType);

        mealLog.setFoodId(null);
        mealLog.setFoodNameSnapshot(customFoodName);
        mealLog.setIntakeG(intakeG);
        mealLog.setCaloriesKcal(kcal);
        mealLog.setCarbG(carb);
        mealLog.setProteinG(protein);
        mealLog.setFatG(fat);

        mealLogRepository.save(mealLog);
    }

    /**
     목표 칼로리설정 메서드
     */
    @Override
    public Map<String, Double> getDietGoalSummary(Long memberId, LocalDate date) {
        List<MealLog> logs = mealLogRepository.findByMemberIdAndMealDate(memberId, date);

        double totalKcal = 0;
        for (MealLog log : logs) {
            totalKcal += log.getCaloriesKcal();
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        double goalKcal = member.getGoalEatKcal() != null ? member.getGoalEatKcal() : 0;
        double remainingKcal = goalKcal - totalKcal;
        double exceededKcal = remainingKcal < 0 ? Math.abs(remainingKcal) : 0;

        double goalPercent = 0;
        if (goalKcal > 0) {
            goalPercent = (totalKcal / goalKcal) * 100.0;
        }

        Map<String, Double> result = new HashMap<>();
        result.put("goalKcal", goalKcal);
        result.put("totalKcal", totalKcal);
        result.put("remainingKcal", Math.max(remainingKcal, 0));
        result.put("exceededKcal", exceededKcal);
        result.put("goalPercent", goalPercent);

        return result;
    }

    /**
     목표칼로리 달성 그래프 메서드
     */
    @Override
    public Map<String, Object> getWeeklyIntakeSummary(Long memberId, LocalDate date) {
        List<String> labels = new ArrayList<>();
        List<Double> calories = new ArrayList<>();
        List<String> colors = new ArrayList<>();

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        double goalKcal = member.getGoalEatKcal() != null ? member.getGoalEatKcal() : 0;

        for (int i = 6; i >= 0; i--) {
            LocalDate targetDate = date.minusDays(i);

            List<MealLog> logs = mealLogRepository.findByMemberIdAndMealDate(memberId, targetDate);

            double totalKcal = 0;
            for (MealLog log : logs) {
                totalKcal += log.getCaloriesKcal();
            }

            labels.add(targetDate.getMonthValue() + "/" + targetDate.getDayOfMonth());
            calories.add(totalKcal);

            if (goalKcal > 0 && totalKcal > goalKcal) {
                colors.add("#e25555");
            } else {
                colors.add("#5b8def");
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("calories", calories);
        result.put("colors", colors);
        result.put("goalKcal", goalKcal);

        return result;
    }
}