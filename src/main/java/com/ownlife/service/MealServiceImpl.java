package com.ownlife.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ownlife.dto.*;
import com.ownlife.entity.Food;
import com.ownlife.entity.MealLog;
import com.ownlife.entity.Member;
import com.ownlife.repository.FoodRepository;
import com.ownlife.repository.MealLogRepository;
import com.ownlife.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
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
    public void addMeal(Long memberId, LocalDate date, String mealType, Long foodId, double count) {

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
        mealLog.setIntakeCount(count);

        // 5. 저장
        mealLogRepository.save(mealLog);
    }

    /*
    식단 담기
    */
    @Override
    public void addMeals(Long memberId, LocalDate date, String mealType, String selectedFoodsJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            List<Map<String, Object>> foods = objectMapper.readValue(
                    selectedFoodsJson,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            for (Map<String, Object> foodData : foods) {
                Long foodId = Long.valueOf(String.valueOf(foodData.get("foodId")));
                double count = Integer.parseInt(String.valueOf(foodData.get("count")));

                addMeal(memberId, date, mealType, foodId, count);
            }
        } catch (Exception e) {
            throw new RuntimeException("식단 저장 중 오류가 발생했습니다.", e);
        }
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
                              double count,
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
        mealLog.setIntakeCount(count);

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
                colors.add("#22c55e");
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("calories", calories);
        result.put("colors", colors);
        result.put("goalKcal", goalKcal);

        return result;
    }

    @Override
    public Map<String, Object> getDietDayChart(Long memberId, LocalDate date) {
        return getWeeklyIntakeSummary(memberId, date);
    }

    @Override
    public Map<String, Object> getDietWeekChart(Long memberId, LocalDate date) {
        List<String> labels = new ArrayList<>();
        List<Double> calories = new ArrayList<>();

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        double goalKcal = member.getGoalEatKcal() != null ? member.getGoalEatKcal() : 0;

        LocalDate startDate = date.minusWeeks(3);
        LocalDate endDate = date.plusDays(6);

        List<MealLog> allLogs = mealLogRepository.findByMemberIdAndMealDateBetween(memberId, startDate, endDate);

        for (int i = 3; i >= 0; i--) {
            LocalDate weekStart = date.minusWeeks(i);
            LocalDate weekEnd = weekStart.plusDays(6);

            List<MealLog> weekLogs = allLogs.stream()
                    .filter(log -> !log.getMealDate().isBefore(weekStart) && !log.getMealDate().isAfter(weekEnd))
                    .toList();

            double totalKcal = weekLogs.stream()
                    .mapToDouble(MealLog::getCaloriesKcal)
                    .sum();

            long recordedDays = weekLogs.stream()
                    .map(MealLog::getMealDate)
                    .distinct()
                    .count();

            double avgKcal = recordedDays > 0 ? totalKcal / recordedDays : 0;

            labels.add(getWeekLabel(weekStart));
            calories.add(avgKcal);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("calories", calories);
        result.put("goalKcal", goalKcal);

        return result;
    }

    @Override
    public Map<String, Object> getDietMonthChart(Long memberId, LocalDate date) {
        List<String> labels = new ArrayList<>();
        List<Double> calories = new ArrayList<>();

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        double goalKcal = member.getGoalEatKcal() != null ? member.getGoalEatKcal() : 0;

        LocalDate startDate = date.minusMonths(5).withDayOfMonth(1);
        LocalDate endDate = date.withDayOfMonth(date.lengthOfMonth());

        List<MealLog> allLogs = mealLogRepository.findByMemberIdAndMealDateBetween(memberId, startDate, endDate);

        for (int i = 5; i >= 0; i--) {
            LocalDate targetMonth = date.minusMonths(i);
            int year = targetMonth.getYear();
            int month = targetMonth.getMonthValue();

            List<MealLog> monthLogs = allLogs.stream()
                    .filter(log -> log.getMealDate().getYear() == year
                            && log.getMealDate().getMonthValue() == month)
                    .toList();

            double totalKcal = monthLogs.stream()
                    .mapToDouble(MealLog::getCaloriesKcal)
                    .sum();

            long recordedDays = monthLogs.stream()
                    .map(MealLog::getMealDate)
                    .distinct()
                    .count();

            double avgKcal = recordedDays > 0 ? totalKcal / recordedDays : 0;

            labels.add(month + "월");
            calories.add(avgKcal);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("calories", calories);
        result.put("goalKcal", goalKcal);

        return result;
    }

    @Override
    public Map<String, Object> getDietYearChart(Long memberId, LocalDate date) {
        List<String> labels = new ArrayList<>();
        List<Double> calories = new ArrayList<>();

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        double goalKcal = member.getGoalEatKcal() != null ? member.getGoalEatKcal() : 0;

        LocalDate startDate = date.minusYears(4).withDayOfYear(1);
        LocalDate endDate = date.withDayOfYear(date.lengthOfYear());

        List<MealLog> allLogs = mealLogRepository.findByMemberIdAndMealDateBetween(memberId, startDate, endDate);

        for (int i = 4; i >= 0; i--) {
            int targetYear = date.minusYears(i).getYear();

            List<MealLog> yearLogs = allLogs.stream()
                    .filter(log -> log.getMealDate().getYear() == targetYear)
                    .toList();

            double totalKcal = yearLogs.stream()
                    .mapToDouble(MealLog::getCaloriesKcal)
                    .sum();

            long recordedDays = yearLogs.stream()
                    .map(MealLog::getMealDate)
                    .distinct()
                    .count();

            double avgKcal = recordedDays > 0 ? totalKcal / recordedDays : 0;

            labels.add(targetYear + "년");
            calories.add(avgKcal);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("calories", calories);
        result.put("goalKcal", goalKcal);

        return result;
    }

    /*
    식단로그 삭제 버튼
     */
    @Transactional
    @Override
    public void deleteMealGroup(Long memberId, LocalDate date, String mealType) {
        mealLogRepository.deleteByMemberIdAndMealDateAndMealType(memberId, date, mealType);
    }

    @Override
    public DietPageDataDto getDietPageData(Long memberId, LocalDate date) {
        List<MealLog> mealLogs = mealLogRepository.findByMemberIdAndMealDate(memberId, date);

        NutritionSummaryDto dailySummary = createNutritionSummary(mealLogs);
        GoalSummaryDto goalSummary = createGoalSummary(memberId, dailySummary);
        List<MealGroupDto> mealGroups = createMealGroups(mealLogs);

        DietChartBundleDto charts = new DietChartBundleDto();
        charts.setDay(convertChartDto(getDietDayChart(memberId, date)));
        charts.setWeek(convertChartDto(getDietWeekChart(memberId, date)));
        charts.setMonth(convertChartDto(getDietMonthChart(memberId, date)));
        charts.setYear(convertChartDto(getDietYearChart(memberId, date)));

        DietPageDataDto dto = new DietPageDataDto();
        dto.setSelectedDate(date);
        dto.setDailySummary(dailySummary);
        dto.setGoalSummary(goalSummary);
        dto.setMealGroups(mealGroups);
        dto.setCharts(charts);

        return dto;
    }

    private DietChartDto convertChartDto(Map<String, Object> chartMap) {
        DietChartDto dto = new DietChartDto();

        dto.setLabels((List<String>) chartMap.getOrDefault("labels", new ArrayList<>()));
        dto.setCalories((List<Double>) chartMap.getOrDefault("calories", new ArrayList<>()));

        Object goalObj = chartMap.get("goalKcal");
        dto.setGoalKcal(goalObj instanceof Number ? ((Number) goalObj).doubleValue() : 0);

        return dto;
    }

    private NutritionSummaryDto createNutritionSummary(List<MealLog> mealLogs) {
        double totalKcal = 0;
        double totalCarb = 0;
        double totalProtein = 0;
        double totalFat = 0;

        for (MealLog mealLog : mealLogs) {
            totalKcal += mealLog.getCaloriesKcal() != null ? mealLog.getCaloriesKcal() : 0;
            totalCarb += mealLog.getCarbG() != null ? mealLog.getCarbG() : 0;
            totalProtein += mealLog.getProteinG() != null ? mealLog.getProteinG() : 0;
            totalFat += mealLog.getFatG() != null ? mealLog.getFatG() : 0;
        }

        NutritionSummaryDto dto = new NutritionSummaryDto();
        dto.setKcal(totalKcal);
        dto.setCarb(totalCarb);
        dto.setProtein(totalProtein);
        dto.setFat(totalFat);

        return dto;
    }

    private GoalSummaryDto createGoalSummary(Long memberId, NutritionSummaryDto dailySummary) {
        double goalKcal = getGoalCalories(memberId);
        double totalKcal = dailySummary.getKcal();

        double remainingKcal = Math.max(goalKcal - totalKcal, 0);
        double exceededKcal = totalKcal > goalKcal ? totalKcal - goalKcal : 0;
        double goalPercent = goalKcal > 0 ? (totalKcal / goalKcal) * 100.0 : 0;

        GoalSummaryDto dto = new GoalSummaryDto();
        dto.setGoalKcal(goalKcal);
        dto.setTotalKcal(totalKcal);
        dto.setRemainingKcal(remainingKcal);
        dto.setExceededKcal(exceededKcal);
        dto.setGoalPercent(goalPercent);

        return dto;
    }

    private List<MealGroupDto> createMealGroups(List<MealLog> mealLogs) {
        Map<String, List<MealLog>> grouped = mealLogs.stream()
                .collect(Collectors.groupingBy(MealLog::getMealType, LinkedHashMap::new, Collectors.toList()));

        List<MealGroupDto> result = new ArrayList<>();

        for (Map.Entry<String, List<MealLog>> entry : grouped.entrySet()) {
            String mealType = entry.getKey();
            List<MealLog> logs = entry.getValue();

            List<MealItemDto> items = new ArrayList<>();
            double subtotalKcal = 0;

            for (MealLog mealLog : logs) {
                MealItemDto itemDto = new MealItemDto();
                itemDto.setMealLogId(mealLog.getMealLogId());
                itemDto.setFoodName(mealLog.getFoodNameSnapshot());
                itemDto.setCaloriesKcal(mealLog.getCaloriesKcal() != null ? mealLog.getCaloriesKcal() : 0);

                items.add(itemDto);
                subtotalKcal += itemDto.getCaloriesKcal();
            }

            MealGroupDto groupDto = new MealGroupDto();
            groupDto.setMealType(mealType);
            groupDto.setMealTypeLabel(convertMealTypeLabel(mealType));
            groupDto.setItems(items);
            groupDto.setSubtotalKcal(subtotalKcal);

            result.add(groupDto);
        }

        result.sort(Comparator.comparingInt(group -> getMealTypeOrder(group.getMealType())));
        return result;
    }

    private String convertMealTypeLabel(String mealType) {
        if (mealType == null) return "";

        switch (mealType) {
            case "BREAKFAST":
                return "아침";
            case "LUNCH":
                return "점심";
            case "DINNER":
                return "저녁";
            case "SNACK":
                return "간식";
            default:
                return mealType;
        }
    }

    private int getMealTypeOrder(String mealType) {
        if (mealType == null) return 99;

        switch (mealType) {
            case "BREAKFAST":
                return 1;
            case "LUNCH":
                return 2;
            case "DINNER":
                return 3;
            case "SNACK":
                return 4;
            default:
                return 99;
        }
    }

    private double getGoalCalories(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        return member.getGoalEatKcal() != null ? member.getGoalEatKcal() : 0;
    }

    private int getWeekOfMonth(LocalDate date) {
        LocalDate firstDayOfMonth = date.withDayOfMonth(1);
        int firstDayValue = firstDayOfMonth.getDayOfWeek().getValue(); // 월=1 ~ 일=7
        int adjustedDay = date.getDayOfMonth() + (firstDayValue - 1);
        return (adjustedDay - 1) / 7 + 1;
    }

    private String getWeekLabel(LocalDate weekStart) {
        return weekStart.getMonthValue() + "월 " + getWeekOfMonth(weekStart) + "주";
    }


}