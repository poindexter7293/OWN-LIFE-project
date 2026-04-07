package com.ownlife.service;

import com.ownlife.dto.DashboardSummaryDto;
import com.ownlife.entity.MealLog;
import com.ownlife.entity.Member;
import com.ownlife.entity.WeightLog;
import com.ownlife.repository.ExerciseLogRepository;
import com.ownlife.repository.MealLogRepository;
import com.ownlife.repository.MemberRepository;
import com.ownlife.repository.WeightLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MemberRepository memberRepository;
    private final MealLogRepository mealLogRepository;
    private final ExerciseLogRepository exerciseLogRepository;
    private final WeightLogRepository weightLogRepository;

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

        return dto;
    }

    private void applyWeightSection(DashboardSummaryDto dto, Long memberId, Member member) {
        List<WeightLog> recentLogs = weightLogRepository.findTop2ByMember_MemberIdOrderByLogDateDesc(memberId);

        if (!recentLogs.isEmpty()) {
            double latestWeight = recentLogs.get(0).getWeightKg().doubleValue();
            dto.setWeight(latestWeight);

            if (recentLogs.size() >= 2) {
                double prevWeight = recentLogs.get(1).getWeightKg().doubleValue();
                double diff = latestWeight - prevWeight;
                dto.setWeightDiff(diff);
                dto.setWeightDiffText(String.format("전일 대비 %+.1fkg", diff));
            } else {
                dto.setWeightDiff(0.0);
                dto.setWeightDiffText("이전 기록 없음");
            }
            return;
        }

        if (member.getWeightKg() != null) {
            dto.setWeight(member.getWeightKg().doubleValue());
            dto.setWeightDiff(0.0);
            dto.setWeightDiffText("체중 로그 없음");
        }
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
        dto.setStreakMessage(streakDays > 0 ? "연속 기록 중" : "오늘 기록 없음");
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
}