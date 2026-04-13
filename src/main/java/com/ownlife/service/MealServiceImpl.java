package com.ownlife.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ownlife.dto.DietChartBundleDto;
import com.ownlife.dto.DietChartDto;
import com.ownlife.dto.DietPageDataDto;
import com.ownlife.dto.GoalSummaryDto;
import com.ownlife.dto.MealGroupDto;
import com.ownlife.dto.MealItemDto;
import com.ownlife.dto.NutritionSummaryDto;
import com.ownlife.entity.Food;
import com.ownlife.entity.MealLog;
import com.ownlife.entity.Member;
import com.ownlife.entity.MemberGoalHistory;
import com.ownlife.repository.FoodRepository;
import com.ownlife.repository.MealLogRepository;
import com.ownlife.repository.MemberGoalHistoryRepository;
import com.ownlife.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealServiceImpl implements MealService {

    private final FoodRepository foodRepository;
    private final MealLogRepository mealLogRepository;
    private final MemberRepository memberRepository;
    private final MemberGoalHistoryRepository memberGoalHistoryRepository;

    @Override
    public List<Food> getAllFoods() {
        return foodRepository.findAll();
    }

    @Override
    public List<MealLog> getMealLogsByDate(Long memberId, LocalDate date) {
        return mealLogRepository.findByMemberIdAndMealDate(memberId, date);
    }

    @Override
    public Map<String, Double> getDailySummary(Long memberId, LocalDate date) {
        List<MealLog> logs = mealLogRepository.findByMemberIdAndMealDate(memberId, date);
        return createDailySummaryMap(logs);
    }

    @Override
    public void addMeal(Long memberId, LocalDate date, String mealType, Long foodId, double count) {
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new IllegalArgumentException("음식 없음"));

        double intakeG = count * food.getBaseAmountG();
        double ratio = intakeG / 100.0;

        double kcal = food.getCaloriesKcal() * ratio;
        double carb = food.getCarbG() * ratio;
        double protein = food.getProteinG() * ratio;
        double fat = food.getFatG() * ratio;

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

        mealLogRepository.save(mealLog);
    }

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
                double count = Double.parseDouble(String.valueOf(foodData.get("count")));
                addMeal(memberId, date, mealType, foodId, count);
            }
        } catch (Exception e) {
            throw new RuntimeException("식단 저장 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void deleteMeal(Long memberId, Long mealLogId) {
        MealLog log = mealLogRepository.findById(mealLogId)
                .orElseThrow(() -> new IllegalArgumentException("기록 없음"));

        if (!log.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("삭제 권한 없음");
        }

        mealLogRepository.delete(log);
    }

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

    @Override
    public Map<String, Double> getDietGoalSummary(Long memberId, LocalDate date) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        List<MealLog> logs = mealLogRepository.findByMemberIdAndMealDate(memberId, date);
        double totalKcal = logs.stream().mapToDouble(log -> safeDouble(log.getCaloriesKcal())).sum();

        return createGoalSummaryMap(member, totalKcal);
    }

    @Override
    public Map<String, Object> getWeeklyIntakeSummary(Long memberId, LocalDate date) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        LocalDate startDate = date.minusDays(6);
        List<MealLog> logs = mealLogRepository.findByMemberIdAndMealDateBetween(memberId, startDate, date);
        List<MemberGoalHistory> histories = memberGoalHistoryRepository
                .findByMember_MemberIdAndChangedAtLessThanEqualOrderByChangedAtAsc(memberId, date.atTime(LocalTime.MAX));

        ChartContext context = createChartContext(member, date, logs, histories);
        return buildDayChart(context);
    }

    @Override
    public Map<String, Object> getDietDayChart(Long memberId, LocalDate date) {
        return getWeeklyIntakeSummary(memberId, date);
    }

    @Override
    public Map<String, Object> getDietWeekChart(Long memberId, LocalDate date) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        LocalDate startDate = date.minusWeeks(3);
        LocalDate endDate = date.plusDays(6);

        List<MealLog> logs = mealLogRepository.findByMemberIdAndMealDateBetween(memberId, startDate, endDate);
        List<MemberGoalHistory> histories = memberGoalHistoryRepository
                .findByMember_MemberIdAndChangedAtLessThanEqualOrderByChangedAtAsc(memberId, date.atTime(LocalTime.MAX));

        ChartContext context = createChartContext(member, date, logs, histories);
        return buildWeekChart(context);
    }

    @Override
    public Map<String, Object> getDietMonthChart(Long memberId, LocalDate date) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        LocalDate startDate = date.minusMonths(5).withDayOfMonth(1);
        LocalDate endDate = date.withDayOfMonth(date.lengthOfMonth());

        List<MealLog> logs = mealLogRepository.findByMemberIdAndMealDateBetween(memberId, startDate, endDate);
        List<MemberGoalHistory> histories = memberGoalHistoryRepository
                .findByMember_MemberIdAndChangedAtLessThanEqualOrderByChangedAtAsc(memberId, date.atTime(LocalTime.MAX));

        ChartContext context = createChartContext(member, date, logs, histories);
        return buildMonthChart(context);
    }

    @Override
    public Map<String, Object> getDietYearChart(Long memberId, LocalDate date) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        LocalDate startDate = date.minusYears(4).withDayOfYear(1);
        LocalDate endDate = date.withDayOfYear(date.lengthOfYear());

        List<MealLog> logs = mealLogRepository.findByMemberIdAndMealDateBetween(memberId, startDate, endDate);
        List<MemberGoalHistory> histories = memberGoalHistoryRepository
                .findByMember_MemberIdAndChangedAtLessThanEqualOrderByChangedAtAsc(memberId, date.atTime(LocalTime.MAX));

        ChartContext context = createChartContext(member, date, logs, histories);
        return buildYearChart(context);
    }

    @Transactional
    @Override
    public void deleteMealGroup(Long memberId, LocalDate date, String mealType) {
        mealLogRepository.deleteByMemberIdAndMealDateAndMealType(memberId, date, mealType);
    }

    @Override
    public DietPageDataDto getDietPageData(Long memberId, LocalDate date) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        List<MealLog> todayMealLogs = mealLogRepository.findByMemberIdAndMealDate(memberId, date);

        LocalDate chartStartDate = date.minusYears(4).withDayOfYear(1);
        LocalDate chartEndDate = date.withDayOfYear(date.lengthOfYear());
        List<MealLog> chartLogs = mealLogRepository.findByMemberIdAndMealDateBetween(memberId, chartStartDate, chartEndDate);

        List<MemberGoalHistory> histories = memberGoalHistoryRepository
                .findByMember_MemberIdAndChangedAtLessThanEqualOrderByChangedAtAsc(memberId, date.atTime(LocalTime.MAX));

        NutritionSummaryDto dailySummary = createNutritionSummary(todayMealLogs);
        GoalSummaryDto goalSummary = createGoalSummary(member, dailySummary);
        List<MealGroupDto> mealGroups = createMealGroups(todayMealLogs);

        ChartContext context = createChartContext(member, date, chartLogs, histories);

        DietChartBundleDto charts = new DietChartBundleDto();
        charts.setDay(convertChartDto(buildDayChart(context)));
        charts.setWeek(convertChartDto(buildWeekChart(context)));
        charts.setMonth(convertChartDto(buildMonthChart(context)));
        charts.setYear(convertChartDto(buildYearChart(context)));

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
        dto.setGoalKcalSeries((List<Double>) chartMap.getOrDefault("goalKcalSeries", new ArrayList<>()));

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
            totalKcal += safeDouble(mealLog.getCaloriesKcal());
            totalCarb += safeDouble(mealLog.getCarbG());
            totalProtein += safeDouble(mealLog.getProteinG());
            totalFat += safeDouble(mealLog.getFatG());
        }

        NutritionSummaryDto dto = new NutritionSummaryDto();
        dto.setKcal(totalKcal);
        dto.setCarb(totalCarb);
        dto.setProtein(totalProtein);
        dto.setFat(totalFat);

        return dto;
    }

    private Map<String, Double> createDailySummaryMap(List<MealLog> mealLogs) {
        Map<String, Double> summary = new HashMap<>();
        NutritionSummaryDto dto = createNutritionSummary(mealLogs);

        summary.put("kcal", dto.getKcal());
        summary.put("carb", dto.getCarb());
        summary.put("protein", dto.getProtein());
        summary.put("fat", dto.getFat());

        return summary;
    }

    private GoalSummaryDto createGoalSummary(Member member, NutritionSummaryDto dailySummary) {
        double goalKcal = getCurrentGoalEatKcal(member);
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

    private Map<String, Double> createGoalSummaryMap(Member member, double totalKcal) {
        double goalKcal = getCurrentGoalEatKcal(member);
        double remainingKcal = goalKcal - totalKcal;
        double exceededKcal = remainingKcal < 0 ? Math.abs(remainingKcal) : 0;
        double goalPercent = goalKcal > 0 ? (totalKcal / goalKcal) * 100.0 : 0;

        Map<String, Double> result = new HashMap<>();
        result.put("goalKcal", goalKcal);
        result.put("totalKcal", totalKcal);
        result.put("remainingKcal", Math.max(remainingKcal, 0));
        result.put("exceededKcal", exceededKcal);
        result.put("goalPercent", goalPercent);

        return result;
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
                itemDto.setCaloriesKcal(safeDouble(mealLog.getCaloriesKcal()));

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

    private ChartContext createChartContext(Member member,
                                            LocalDate baseDate,
                                            List<MealLog> chartLogs,
                                            List<MemberGoalHistory> histories) {

        Map<LocalDate, Double> dailyCalories = new HashMap<>();

        for (MealLog log : chartLogs) {
            LocalDate mealDate = log.getMealDate();
            dailyCalories.merge(mealDate, safeDouble(log.getCaloriesKcal()), Double::sum);
        }

        return new ChartContext(member, baseDate, dailyCalories, histories != null ? histories : new ArrayList<>());
    }

    private Map<String, Object> buildDayChart(ChartContext context) {
        List<String> labels = new ArrayList<>();
        List<Double> calories = new ArrayList<>();
        List<String> colors = new ArrayList<>();
        List<Double> goalKcalSeries = new ArrayList<>();

        LocalDate startDate = context.baseDate.minusDays(6);

        for (LocalDate targetDate = startDate; !targetDate.isAfter(context.baseDate); targetDate = targetDate.plusDays(1)) {
            double totalKcal = context.dailyCaloriesByDate.getOrDefault(targetDate, 0.0);
            double goalKcal = targetDate.equals(context.baseDate)
                    ? getCurrentGoalEatKcal(context.member)
                    : resolveGoalEatKcalForDate(context.member, context.histories, targetDate);

            labels.add(targetDate.getMonthValue() + "/" + targetDate.getDayOfMonth());
            calories.add(totalKcal);
            goalKcalSeries.add(goalKcal);
            colors.add(goalKcal > 0 && totalKcal > goalKcal ? "#e25555" : "#22c55e");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("calories", calories);
        result.put("colors", colors);
        result.put("goalKcal", getCurrentGoalEatKcal(context.member));
        result.put("goalKcalSeries", goalKcalSeries);

        return result;
    }

    private Map<String, Object> buildWeekChart(ChartContext context) {
        List<String> labels = new ArrayList<>();
        List<Double> calories = new ArrayList<>();
        List<Double> goalKcalSeries = new ArrayList<>();

        for (int i = 3; i >= 0; i--) {
            LocalDate weekStart = context.baseDate.minusWeeks(i);
            LocalDate weekEnd = weekStart.plusDays(6);

            double totalKcal = 0;
            int recordedDays = 0;

            for (LocalDate d = weekStart; !d.isAfter(weekEnd); d = d.plusDays(1)) {
                Double dayKcal = context.dailyCaloriesByDate.get(d);
                if (dayKcal != null) {
                    totalKcal += dayKcal;
                    recordedDays++;
                }
            }

            double avgKcal = recordedDays > 0 ? totalKcal / recordedDays : 0;

            labels.add(getWeekLabel(weekStart));
            calories.add(avgKcal);

            if (i == 0) {
                goalKcalSeries.add(getCurrentGoalEatKcal(context.member));
            } else {
                goalKcalSeries.add(averageGoalKcalForRange(context.member, context.histories, weekStart, weekEnd));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("calories", calories);
        result.put("goalKcal", getCurrentGoalEatKcal(context.member));
        result.put("goalKcalSeries", goalKcalSeries);

        return result;
    }

    private Map<String, Object> buildMonthChart(ChartContext context) {
        List<String> labels = new ArrayList<>();
        List<Double> calories = new ArrayList<>();
        List<Double> goalKcalSeries = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            LocalDate targetMonth = context.baseDate.minusMonths(i);
            LocalDate monthStart = targetMonth.withDayOfMonth(1);
            LocalDate monthEnd = targetMonth.withDayOfMonth(targetMonth.lengthOfMonth());

            double totalKcal = 0;
            int recordedDays = 0;

            for (LocalDate d = monthStart; !d.isAfter(monthEnd); d = d.plusDays(1)) {
                Double dayKcal = context.dailyCaloriesByDate.get(d);
                if (dayKcal != null) {
                    totalKcal += dayKcal;
                    recordedDays++;
                }
            }

            double avgKcal = recordedDays > 0 ? totalKcal / recordedDays : 0;

            labels.add(targetMonth.getMonthValue() + "월");
            calories.add(avgKcal);

            if (i == 0) {
                goalKcalSeries.add(getCurrentGoalEatKcal(context.member));
            } else {
                goalKcalSeries.add(averageGoalKcalForRange(context.member, context.histories, monthStart, monthEnd));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("calories", calories);
        result.put("goalKcal", getCurrentGoalEatKcal(context.member));
        result.put("goalKcalSeries", goalKcalSeries);

        return result;
    }

    private Map<String, Object> buildYearChart(ChartContext context) {
        List<String> labels = new ArrayList<>();
        List<Double> calories = new ArrayList<>();
        List<Double> goalKcalSeries = new ArrayList<>();

        for (int i = 4; i >= 0; i--) {
            LocalDate targetYear = context.baseDate.minusYears(i);
            LocalDate yearStart = targetYear.withDayOfYear(1);
            LocalDate yearEnd = targetYear.withDayOfYear(targetYear.lengthOfYear());

            double totalKcal = 0;
            int recordedDays = 0;

            for (LocalDate d = yearStart; !d.isAfter(yearEnd); d = d.plusDays(1)) {
                Double dayKcal = context.dailyCaloriesByDate.get(d);
                if (dayKcal != null) {
                    totalKcal += dayKcal;
                    recordedDays++;
                }
            }

            double avgKcal = recordedDays > 0 ? totalKcal / recordedDays : 0;

            labels.add(targetYear.getYear() + "년");
            calories.add(avgKcal);

            if (i == 0) {
                goalKcalSeries.add(getCurrentGoalEatKcal(context.member));
            } else {
                goalKcalSeries.add(averageGoalKcalForRange(context.member, context.histories, yearStart, yearEnd));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("calories", calories);
        result.put("goalKcal", getCurrentGoalEatKcal(context.member));
        result.put("goalKcalSeries", goalKcalSeries);

        return result;
    }

    private double averageGoalKcalForRange(Member member,
                                           List<MemberGoalHistory> histories,
                                           LocalDate startDate,
                                           LocalDate endDate) {
        double totalGoalKcal = 0;
        int days = 0;

        for (LocalDate targetDate = startDate; !targetDate.isAfter(endDate); targetDate = targetDate.plusDays(1)) {
            totalGoalKcal += resolveGoalEatKcalForDate(member, histories, targetDate);
            days++;
        }

        return days > 0 ? totalGoalKcal / days : 0;
    }

    private double resolveGoalEatKcalForDate(Member member,
                                             List<MemberGoalHistory> histories,
                                             LocalDate targetDate) {
        LocalDateTime endOfDay = targetDate.atTime(LocalTime.MAX);
        double fallback = getCurrentGoalEatKcal(member);

        if (histories == null || histories.isEmpty()) {
            return fallback;
        }

        Double resolved = null;
        for (MemberGoalHistory history : histories) {
            if (history.getChangedAt() == null || history.getChangedAt().isAfter(endOfDay)) {
                break;
            }
            if (history.getGoalEatKcal() != null) {
                resolved = history.getGoalEatKcal().doubleValue();
            }
        }

        return resolved != null ? resolved : fallback;
    }

    private double getCurrentGoalEatKcal(Member member) {
        return member.getGoalEatKcal() != null ? member.getGoalEatKcal().doubleValue() : 0.0;
    }

    private double safeDouble(Number value) {
        return value != null ? value.doubleValue() : 0.0;
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

    private String getWeekLabel(LocalDate weekStart) {
        return weekStart.getMonthValue() + "/" + weekStart.getDayOfMonth() + "주";
    }

    private static class ChartContext {
        private final Member member;
        private final LocalDate baseDate;
        private final Map<LocalDate, Double> dailyCaloriesByDate;
        private final List<MemberGoalHistory> histories;

        private ChartContext(Member member,
                             LocalDate baseDate,
                             Map<LocalDate, Double> dailyCaloriesByDate,
                             List<MemberGoalHistory> histories) {
            this.member = member;
            this.baseDate = baseDate;
            this.dailyCaloriesByDate = dailyCaloriesByDate;
            this.histories = histories;
        }
    }
}