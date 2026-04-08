package com.ownlife.service;

import com.ownlife.dto.LifestylePatternAnalysisDto;
import com.ownlife.entity.ExerciseLog;
import com.ownlife.entity.MealLog;
import com.ownlife.entity.Member;
import com.ownlife.repository.ExerciseLogRepository;
import com.ownlife.repository.MealLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LifestylePatternServiceTest {

    @Test
    @DisplayName("최근 기록이 거의 없으면 분석 준비 단계 메시지를 반환한다")
    void analyzeReturnsWarmupMessageWhenDataIsInsufficient() {
        ExerciseLogRepository exerciseLogRepository = mock(ExerciseLogRepository.class);
        MealLogRepository mealLogRepository = mock(MealLogRepository.class);
        LifestylePatternService service = new LifestylePatternService(exerciseLogRepository, mealLogRepository);

        when(exerciseLogRepository.findByMember_MemberIdAndExerciseDateBetweenOrderByExerciseDateAscCreatedAtAsc(eq(1L), any(), any()))
                .thenReturn(List.of());
        when(mealLogRepository.findByMemberIdAndMealDateBetween(eq(1L), any(), any()))
                .thenReturn(List.of());

        LifestylePatternAnalysisDto result = service.analyze(1L);

        assertEquals("아직 생활 패턴을 분석하는 중이에요", result.getTitle());
        assertTrue(result.hasInsights());
        assertEquals(2, result.getInsights().size());
    }

    @Test
    @DisplayName("주말 밤 운동과 식단 누락이 많으면 관련 패턴 문구를 반환한다")
    void analyzeBuildsWeekendNightAndDietWeakInsights() {
        ExerciseLogRepository exerciseLogRepository = mock(ExerciseLogRepository.class);
        MealLogRepository mealLogRepository = mock(MealLogRepository.class);
        LifestylePatternService service = new LifestylePatternService(exerciseLogRepository, mealLogRepository);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(27);

        List<ExerciseLog> exerciseLogs = buildWeekendNightExerciseLogs(startDate, endDate);
        List<MealLog> mealLogs = buildSparseMealLogs(startDate, endDate);

        when(exerciseLogRepository.findByMember_MemberIdAndExerciseDateBetweenOrderByExerciseDateAscCreatedAtAsc(eq(1L), any(), any()))
                .thenReturn(exerciseLogs);
        when(mealLogRepository.findByMemberIdAndMealDateBetween(eq(1L), any(), any()))
                .thenReturn(mealLogs);

        LifestylePatternAnalysisDto result = service.analyze(1L);

        List<String> insightTitles = result.getInsights().stream()
                .map(insight -> insight.getTitle())
                .toList();

        assertTrue(result.getTitle().contains("주말 몰아형"));
        assertTrue(insightTitles.contains("야행성 기록 패턴"));
        assertTrue(insightTitles.contains("주말 몰아 운동형"));
        assertTrue(insightTitles.contains("운동은 꾸준한데 식단 기록은 약한 편"));
    }

    private List<ExerciseLog> buildWeekendNightExerciseLogs(LocalDate startDate, LocalDate endDate) {
        List<ExerciseLog> logs = new ArrayList<>();
        long idSequence = 1L;

        for (LocalDate cursor = startDate; !cursor.isAfter(endDate); cursor = cursor.plusDays(1)) {
            if (cursor.getDayOfWeek() != DayOfWeek.SATURDAY && cursor.getDayOfWeek() != DayOfWeek.SUNDAY) {
                continue;
            }

            ExerciseLog exerciseLog = new ExerciseLog();
            exerciseLog.setExerciseLogId(idSequence++);
            exerciseLog.setExerciseDate(cursor);
            exerciseLog.setCreatedAt(cursor.atTime(22, 30));
            exerciseLog.setBurnedKcal(BigDecimal.valueOf(320));
            Member member = new Member();
            member.setMemberId(1L);
            exerciseLog.setMember(member);
            logs.add(exerciseLog);
        }

        return logs;
    }

    private List<MealLog> buildSparseMealLogs(LocalDate startDate, LocalDate endDate) {
        List<MealLog> logs = new ArrayList<>();
        long idSequence = 1L;

        for (LocalDate cursor = startDate; !cursor.isAfter(endDate); cursor = cursor.plusDays(1)) {
            if (cursor.getDayOfWeek() == DayOfWeek.MONDAY) {
                continue;
            }

            MealLog lunch = new MealLog();
            lunch.setMealLogId(idSequence++);
            lunch.setMemberId(1L);
            lunch.setMealDate(cursor);
            lunch.setMealType("LUNCH");
            lunch.setFoodNameSnapshot("샐러드");
            lunch.setIntakeG(200.0);
            lunch.setCaloriesKcal(420.0);
            lunch.setCarbG(30.0);
            lunch.setProteinG(20.0);
            lunch.setFatG(10.0);
            logs.add(lunch);
        }

        return logs;
    }
}

