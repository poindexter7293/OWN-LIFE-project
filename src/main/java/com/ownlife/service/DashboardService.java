package com.ownlife.service;

import com.ownlife.dto.DashboardSummaryDto;
import com.ownlife.entity.DailySummary;
import com.ownlife.entity.MealLog;
import com.ownlife.entity.Member;
import com.ownlife.entity.WeightLog;
import com.ownlife.repository.DailySummaryRepository;
import com.ownlife.repository.ExerciseLogRepository;
import com.ownlife.repository.MealLogRepository;
import com.ownlife.repository.MemberRepository;
import com.ownlife.repository.WeightLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MemberRepository memberRepository;
    private final MealLogRepository mealLogRepository;
    private final ExerciseLogRepository exerciseLogRepository;
    private final WeightLogRepository weightLogRepository;
    private final DailySummaryRepository dailySummaryRepository;

    public DashboardSummaryDto getDashboardSummary(Long memberId) {
        DashboardSummaryDto dto = new DashboardSummaryDto();
        LocalDate today = LocalDate.now();

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        applyWeightSection(dto, memberId, member);
        applyExerciseSection(dto, memberId, member, today);
        applyMealSection(dto, memberId, member, today);
        applyStreakSection(dto, memberId, today);
        applyWeeklyWeightSection(dto, memberId, today);
        applySummaryChartSection(dto, memberId, member, today);

        return dto;
    }

    private void applyWeightSection(DashboardSummaryDto dto, Long memberId, Member member) {
        List<WeightLog> recentLogs = weightLogRepository.findTop2ByMember_MemberIdOrderByLogDateDesc(memberId);

        Double currentWeight = null;

        if (!recentLogs.isEmpty() && recentLogs.get(0).getWeightKg() != null) {
            currentWeight = recentLogs.get(0).getWeightKg().doubleValue();
        } else if (member.getWeightKg() != null) {
            currentWeight = member.getWeightKg().doubleValue();
        }

        dto.setWeight(currentWeight != null ? currentWeight : 0.0);
        dto.setWeightDiff(0.0);
        dto.setWeightDiffTone("muted");
        dto.setWeightDiffText("체중 로그 없음");
        dto.setGoalWeightText("목표체중 미설정");

        if (currentWeight == null) {
            dto.setWeightDiffText("체중 로그 없음");
            return;
        }

        Double goalWeight = member.getGoalWeight() != null
                ? member.getGoalWeight().doubleValue()
                : null;

        if (goalWeight != null && goalWeight > 0) {
            dto.setGoalWeightText("목표체중 : " + formatKg(goalWeight) + "kg");
        } else {
            dto.setGoalWeightText("목표체중 미설정");
        }

        if (goalWeight == null || goalWeight <= 0) {
            dto.setWeightDiffText("목표 체중 미설정");
            return;
        }

        double signedDiff = goalWeight - currentWeight;
        double absDiff = Math.abs(signedDiff);

        dto.setWeightDiff(absDiff);

        long currentWeightInt = (long) Math.floor(currentWeight);
        long goalWeightInt = (long) Math.floor(goalWeight);

        String signedDiffText = (signedDiff > 0 ? "+" : "") + formatKg(signedDiff) + "kg";

        if (currentWeightInt == goalWeightInt) {
            dto.setWeightDiffTone("accent");
            dto.setWeightDiffText("목표체중 달성!");
            return;
        }

        if (absDiff <= 3.0) {
            dto.setWeightDiffTone("accent");
            dto.setWeightDiffText("달성 임박! 목표 체중까지 " + signedDiffText);
            return;
        }

        dto.setWeightDiffTone("muted");
        dto.setWeightDiffText("목표 체중까지 " + signedDiffText);
    }

    private void applyExerciseSection(DashboardSummaryDto dto, Long memberId, Member member, LocalDate today) {
        int burnedCalories = Optional.ofNullable(
                        exerciseLogRepository.sumBurnedKcalByMemberAndDate(memberId, today)
                )
                .orElse(BigDecimal.ZERO)
                .intValue();

        int burnedTargetCalories = Optional.ofNullable(member.getGoalBurnedKcal()).orElse(0);
        int burnedPercent = burnedTargetCalories > 0
                ? (int) Math.round((burnedCalories * 100.0) / burnedTargetCalories)
                : 0;

        dto.setBurnedCalories(burnedCalories);
        dto.setBurnedTargetCalories(burnedTargetCalories);
        dto.setBurnedPercent(burnedPercent);
    }

    private void applyMealSection(DashboardSummaryDto dto, Long memberId, Member member, LocalDate today) {
        List<MealLog> mealLogs = mealLogRepository.findByMemberIdAndMealDate(memberId, today);

        int intakeCalories = (int) Math.round(
                mealLogs.stream().mapToDouble(MealLog::getCaloriesKcal).sum()
        );
        int carbGram = (int) Math.round(
                mealLogs.stream().mapToDouble(MealLog::getCarbG).sum()
        );
        int fatGram = (int) Math.round(
                mealLogs.stream().mapToDouble(MealLog::getFatG).sum()
        );
        int proteinGram = (int) Math.round(
                mealLogs.stream().mapToDouble(MealLog::getProteinG).sum()
        );

        int intakeTargetCalories = Optional.ofNullable(member.getGoalEatKcal()).orElse(0);
        int intakePercent = intakeTargetCalories > 0
                ? (int) Math.round((intakeCalories * 100.0) / intakeTargetCalories)
                : 0;

        int macroTotal = carbGram + fatGram + proteinGram;
        int carbPercent = macroTotal > 0 ? (int) Math.round(carbGram * 100.0 / macroTotal) : 0;
        int fatPercent = macroTotal > 0 ? (int) Math.round(fatGram * 100.0 / macroTotal) : 0;
        int proteinPercent = macroTotal > 0 ? Math.max(0, 100 - carbPercent - fatPercent) : 0;

        dto.setIntakeCalories(intakeCalories);
        dto.setIntakeTargetCalories(intakeTargetCalories);
        dto.setIntakePercent(intakePercent);

        dto.setCarbGram(carbGram);
        dto.setFatGram(fatGram);
        dto.setProteinGram(proteinGram);

        dto.setCarbPercent(carbPercent);
        dto.setFatPercent(fatPercent);
        dto.setProteinPercent(proteinPercent);
    }

    private void applyStreakSection(DashboardSummaryDto dto, Long memberId, LocalDate today) {
        LocalDate startDate = today.minusDays(365);

        Set<LocalDate> recordedDates = new HashSet<>();
        recordedDates.addAll(mealLogRepository.findDistinctMealDatesBetween(memberId, startDate, today));
        recordedDates.addAll(exerciseLogRepository.findDistinctExerciseDatesBetween(memberId, startDate, today));
        recordedDates.addAll(
                weightLogRepository.findByMember_MemberIdAndLogDateBetweenOrderByLogDateAsc(memberId, startDate, today)
                        .stream()
                        .map(WeightLog::getLogDate)
                        .collect(Collectors.toSet())
        );

        int streakDays = 0;
        LocalDate cursor = today;

        while (recordedDates.contains(cursor)) {
            streakDays++;
            cursor = cursor.minusDays(1);
        }

        dto.setStreakDays(streakDays);

        if (streakDays > 0) {
            dto.setStreakMessage("연속 기록 중");
            dto.setStreakTone("accent");
        } else {
            dto.setStreakMessage("오늘 기록 없음");
            dto.setStreakTone("danger");
        }
    }

    private void applyWeeklyWeightSection(DashboardSummaryDto dto, Long memberId, LocalDate today) {
        LocalDate startDate = today.minusDays(6);

        Map<LocalDate, Double> weightMap = weightLogRepository
                .findByMember_MemberIdAndLogDateBetweenOrderByLogDateAsc(memberId, startDate, today)
                .stream()
                .collect(Collectors.toMap(
                        WeightLog::getLogDate,
                        log -> log.getWeightKg().doubleValue(),
                        (a, b) -> b,
                        LinkedHashMap::new
                ));

        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            labels.add(date.getMonthValue() + "/" + date.getDayOfMonth());
            data.add(weightMap.get(date));
        }

        dto.setWeightLabels(labels);
        dto.setWeightData(data);
    }

    private void applySummaryChartSection(DashboardSummaryDto dto, Long memberId, Member member, LocalDate today) {
        DashboardSummaryDto.SummaryChartDto chartDto = new DashboardSummaryDto.SummaryChartDto();

        chartDto.setWeek(buildDailyRangeChart(memberId, member, today.minusDays(6), today));
        chartDto.setMonth(buildDailyRangeChart(memberId, member, today.minusDays(29), today));
        chartDto.setYear(buildYearlyRangeChart(memberId, member, today));

        dto.setSummaryChart(chartDto);
    }

    private DashboardSummaryDto.RangeChartDto buildDailyRangeChart(Long memberId,
                                                                   Member member,
                                                                   LocalDate startDate,
                                                                   LocalDate endDate) {
        DashboardSummaryDto.RangeChartDto dto = new DashboardSummaryDto.RangeChartDto();

        List<DailySummary> summaries = dailySummaryRepository
                .findByMember_MemberIdAndSummaryDateBetweenOrderBySummaryDateAsc(memberId, startDate, endDate);

        Map<LocalDate, DailySummary> summaryMap = summaries.stream()
                .collect(Collectors.toMap(
                        DailySummary::getSummaryDate,
                        Function.identity(),
                        (a, b) -> b,
                        LinkedHashMap::new
                ));

        Double currentWeight = resolveLatestWeightBeforeOrEqual(memberId, member, startDate.minusDays(1));
        Integer currentGoalEat = resolveLatestGoalEatBeforeOrEqual(memberId, member, startDate.minusDays(1));
        Integer currentGoalBurned = resolveLatestGoalBurnedBeforeOrEqual(memberId, member, startDate.minusDays(1));
        Double currentGoalWeight = resolveLatestGoalWeightBeforeOrEqual(memberId, member, startDate.minusDays(1));

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DailySummary summary = summaryMap.get(date);

            dto.getLabels().add(date.getMonthValue() + "/" + date.getDayOfMonth());

            if (summary != null && summary.getWeightKg() != null) {
                currentWeight = summary.getWeightKg().doubleValue();
            }
            dto.getWeightKg().add(round2(currentWeight));

            dto.getTotalEatKcal().add(summary != null ? toDouble(summary.getTotalEatKcal(), 0.0) : 0.0);
            dto.getTotalBurnedKcal().add(summary != null ? toDouble(summary.getTotalBurnedKcal(), 0.0) : 0.0);
            dto.getTotalCarbG().add(summary != null ? toDouble(summary.getTotalCarbG(), 0.0) : 0.0);
            dto.getTotalProteinG().add(summary != null ? toDouble(summary.getTotalProteinG(), 0.0) : 0.0);
            dto.getTotalFatG().add(summary != null ? toDouble(summary.getTotalFatG(), 0.0) : 0.0);

            if (summary != null && summary.getGoalEatKcal() != null) {
                currentGoalEat = summary.getGoalEatKcal();
            }
            if (summary != null && summary.getGoalBurnedKcal() != null) {
                currentGoalBurned = summary.getGoalBurnedKcal();
            }
            if (summary != null && summary.getGoalWeight() != null) {
                currentGoalWeight = summary.getGoalWeight().doubleValue();
            }

            dto.getGoalEatKcal().add(currentGoalEat != null ? currentGoalEat.doubleValue() : null);
            dto.getGoalBurnedKcal().add(currentGoalBurned != null ? currentGoalBurned.doubleValue() : null);
            dto.getGoalWeight().add(round2(currentGoalWeight));
        }

        return dto;
    }

    private DashboardSummaryDto.RangeChartDto buildYearlyRangeChart(Long memberId,
                                                                    Member member,
                                                                    LocalDate today) {
        DashboardSummaryDto.RangeChartDto dto = new DashboardSummaryDto.RangeChartDto();

        YearMonth startMonth = YearMonth.from(today).minusMonths(11);
        YearMonth endMonth = YearMonth.from(today);

        LocalDate rangeStart = startMonth.atDay(1);
        LocalDate rangeEnd = endMonth.atEndOfMonth();

        List<DailySummary> summaries = dailySummaryRepository
                .findByMember_MemberIdAndSummaryDateBetweenOrderBySummaryDateAsc(memberId, rangeStart, rangeEnd);

        Map<YearMonth, List<DailySummary>> byMonth = summaries.stream()
                .collect(Collectors.groupingBy(
                        summary -> YearMonth.from(summary.getSummaryDate()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Double currentWeight = resolveLatestWeightBeforeOrEqual(memberId, member, rangeStart.minusDays(1));
        Integer currentGoalEat = resolveLatestGoalEatBeforeOrEqual(memberId, member, rangeStart.minusDays(1));
        Integer currentGoalBurned = resolveLatestGoalBurnedBeforeOrEqual(memberId, member, rangeStart.minusDays(1));
        Double currentGoalWeight = resolveLatestGoalWeightBeforeOrEqual(memberId, member, rangeStart.minusDays(1));

        for (YearMonth month = startMonth; !month.isAfter(endMonth); month = month.plusMonths(1)) {
            List<DailySummary> monthRows = byMonth.getOrDefault(month, Collections.emptyList());

            dto.getLabels().add(month.getMonthValue() + "월");

            if (!monthRows.isEmpty()) {
                DailySummary lastWeightRow = monthRows.stream()
                        .filter(row -> row.getWeightKg() != null)
                        .reduce((first, second) -> second)
                        .orElse(null);

                if (lastWeightRow != null) {
                    currentWeight = lastWeightRow.getWeightKg().doubleValue();
                }

                DailySummary lastGoalEatRow = monthRows.stream()
                        .filter(row -> row.getGoalEatKcal() != null)
                        .reduce((first, second) -> second)
                        .orElse(null);

                if (lastGoalEatRow != null) {
                    currentGoalEat = lastGoalEatRow.getGoalEatKcal();
                }

                DailySummary lastGoalBurnedRow = monthRows.stream()
                        .filter(row -> row.getGoalBurnedKcal() != null)
                        .reduce((first, second) -> second)
                        .orElse(null);

                if (lastGoalBurnedRow != null) {
                    currentGoalBurned = lastGoalBurnedRow.getGoalBurnedKcal();
                }

                DailySummary lastGoalWeightRow = monthRows.stream()
                        .filter(row -> row.getGoalWeight() != null)
                        .reduce((first, second) -> second)
                        .orElse(null);

                if (lastGoalWeightRow != null) {
                    currentGoalWeight = lastGoalWeightRow.getGoalWeight().doubleValue();
                }
            }

            dto.getWeightKg().add(round2(currentWeight));

            dto.getTotalEatKcal().add(avgIfPresent(monthRows, DailySummary::getTotalEatKcal));
            dto.getTotalBurnedKcal().add(avgIfPresent(monthRows, DailySummary::getTotalBurnedKcal));
            dto.getTotalCarbG().add(avgIfPresent(monthRows, DailySummary::getTotalCarbG));
            dto.getTotalProteinG().add(avgIfPresent(monthRows, DailySummary::getTotalProteinG));
            dto.getTotalFatG().add(avgIfPresent(monthRows, DailySummary::getTotalFatG));

            dto.getGoalEatKcal().add(currentGoalEat != null ? currentGoalEat.doubleValue() : null);
            dto.getGoalBurnedKcal().add(currentGoalBurned != null ? currentGoalBurned.doubleValue() : null);
            dto.getGoalWeight().add(round2(currentGoalWeight));
        }

        return dto;
    }

    private Double avgIfPresent(List<DailySummary> rows, Function<DailySummary, BigDecimal> extractor) {
        List<Double> values = rows.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .map(BigDecimal::doubleValue)
                .toList();

        if (values.isEmpty()) {
            return null;
        }

        double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return round2(avg);
    }

    private Double resolveLatestWeightBeforeOrEqual(Long memberId, Member member, LocalDate date) {
        Optional<DailySummary> latestSummary = dailySummaryRepository
                .findFirstByMember_MemberIdAndSummaryDateLessThanEqualOrderBySummaryDateDesc(memberId, date);

        if (latestSummary.isPresent() && latestSummary.get().getWeightKg() != null) {
            return latestSummary.get().getWeightKg().doubleValue();
        }

        Optional<WeightLog> latestWeightLog = weightLogRepository
                .findFirstByMember_MemberIdAndLogDateLessThanEqualOrderByLogDateDesc(memberId, date);

        if (latestWeightLog.isPresent() && latestWeightLog.get().getWeightKg() != null) {
            return latestWeightLog.get().getWeightKg().doubleValue();
        }

        if (member.getWeightKg() != null) {
            return member.getWeightKg().doubleValue();
        }

        return null;
    }

    private Integer resolveLatestGoalEatBeforeOrEqual(Long memberId, Member member, LocalDate date) {
        Optional<DailySummary> latestSummary = dailySummaryRepository
                .findFirstByMember_MemberIdAndSummaryDateLessThanEqualOrderBySummaryDateDesc(memberId, date);

        if (latestSummary.isPresent() && latestSummary.get().getGoalEatKcal() != null) {
            return latestSummary.get().getGoalEatKcal();
        }

        return member.getGoalEatKcal();
    }

    private Integer resolveLatestGoalBurnedBeforeOrEqual(Long memberId, Member member, LocalDate date) {
        Optional<DailySummary> latestSummary = dailySummaryRepository
                .findFirstByMember_MemberIdAndSummaryDateLessThanEqualOrderBySummaryDateDesc(memberId, date);

        if (latestSummary.isPresent() && latestSummary.get().getGoalBurnedKcal() != null) {
            return latestSummary.get().getGoalBurnedKcal();
        }

        return member.getGoalBurnedKcal();
    }

    private Double resolveLatestGoalWeightBeforeOrEqual(Long memberId, Member member, LocalDate date) {
        Optional<DailySummary> latestSummary = dailySummaryRepository
                .findFirstByMember_MemberIdAndSummaryDateLessThanEqualOrderBySummaryDateDesc(memberId, date);

        if (latestSummary.isPresent() && latestSummary.get().getGoalWeight() != null) {
            return latestSummary.get().getGoalWeight().doubleValue();
        }

        if (member.getGoalWeight() != null) {
            return member.getGoalWeight().doubleValue();
        }

        return null;
    }

    private Double toDouble(BigDecimal value, Double defaultValue) {
        return value != null ? round2(value.doubleValue()) : defaultValue;
    }

    private Double round2(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 100.0) / 100.0;
    }

    private String formatKg(double value) {
        double rounded = Math.round(value * 10.0) / 10.0;

        if (rounded == Math.floor(rounded)) {
            return String.valueOf((long) rounded);
        }

        return String.format("%.1f", rounded);
    }
}