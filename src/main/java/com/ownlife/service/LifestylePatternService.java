package com.ownlife.service;

import com.ownlife.dto.LifestyleInsightDto;
import com.ownlife.dto.LifestylePatternAnalysisDto;
import com.ownlife.entity.ExerciseLog;
import com.ownlife.entity.MealLog;
import com.ownlife.repository.ExerciseLogRepository;
import com.ownlife.repository.MealLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LifestylePatternService {

    private static final int ANALYSIS_DAYS = 28;
    private static final Set<String> MAIN_MEAL_TYPES = Set.of("BREAKFAST", "LUNCH", "DINNER");

    private final ExerciseLogRepository exerciseLogRepository;
    private final MealLogRepository mealLogRepository;

    public LifestylePatternAnalysisDto analyze(Long memberId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(ANALYSIS_DAYS - 1L);

        List<ExerciseLog> exerciseLogs = memberId == null
                ? List.of()
                : exerciseLogRepository.findByMember_MemberIdAndExerciseDateBetweenOrderByExerciseDateAscCreatedAtAsc(memberId, startDate, endDate);
        List<MealLog> mealLogs = memberId == null
                ? List.of()
                : mealLogRepository.findByMemberIdAndMealDateBetween(memberId, startDate, endDate);

        Set<LocalDate> recordedDates = new HashSet<>();
        exerciseLogs.stream()
                .map(ExerciseLog::getExerciseDate)
                .filter(java.util.Objects::nonNull)
                .forEach(recordedDates::add);
        mealLogs.stream()
                .map(MealLog::getMealDate)
                .filter(java.util.Objects::nonNull)
                .forEach(recordedDates::add);

        if (recordedDates.size() < 5) {
            return LifestylePatternAnalysisDto.builder()
                    .periodLabel("최근 28일 기준")
                    .title("아직 생활 패턴을 분석하는 중이에요")
                    .description("운동과 식단 기록이 조금 더 쌓이면 요일·시간대별 습관을 더 정확하게 보여드릴게요.")
                    .insights(List.of(
                            LifestyleInsightDto.builder()
                                    .title("분석 준비 단계")
                                    .description("최근 28일 안에 기록된 날짜가 5일 이상이면 생활 패턴 문구가 더 구체적으로 나타나요.")
                                    .tone("muted")
                                    .build(),
                            LifestyleInsightDto.builder()
                                    .title("분석 기준")
                                    .description("운동은 기록 시각과 소모 칼로리, 식단은 아침·점심·저녁 기록 여부를 바탕으로 루틴을 추정해요.")
                                    .tone("accent")
                                    .build()
                    ))
                    .build();
        }

        TimePatternInsight timePatternInsight = analyzeTimePattern(exerciseLogs);
        ActivityPatternInsight activityPatternInsight = analyzeActivityPattern(exerciseLogs, startDate, endDate);
        LifestyleInsightDto mealPatternInsight = analyzeMealPattern(mealLogs, startDate, endDate);
        LifestyleInsightDto balancePatternInsight = analyzeBalancePattern(exerciseLogs, mealLogs, startDate, endDate);

        List<LifestyleInsightDto> insights = new ArrayList<>();
        insights.add(timePatternInsight.insight());
        insights.add(activityPatternInsight.insight());
        insights.add(mealPatternInsight);
        insights.add(balancePatternInsight);

        String title = buildSummaryTitle(timePatternInsight, activityPatternInsight, balancePatternInsight);
        String description = String.format(
                Locale.KOREA,
                "최근 %d일 동안 운동 기록 %d일, 식단 기록 %d일을 바탕으로 생활 리듬을 정리했어요.",
                ANALYSIS_DAYS,
                distinctExerciseDays(exerciseLogs).size(),
                distinctMealDays(mealLogs).size()
        );

        return LifestylePatternAnalysisDto.builder()
                .periodLabel("최근 28일 기준")
                .title(title)
                .description(description)
                .insights(insights)
                .build();
    }

    private String buildSummaryTitle(TimePatternInsight timePatternInsight,
                                     ActivityPatternInsight activityPatternInsight,
                                     LifestyleInsightDto balancePatternInsight) {
        String timeSummary = timePatternInsight.summaryKeyword();
        String activitySummary = activityPatternInsight.summaryKeyword();

        if ("운동은 꾸준한데 식단 기록은 약한 편".equals(balancePatternInsight.getTitle())) {
            return String.format("%s %s + 식단 보완형 패턴이 보여요", activitySummary, timeSummary);
        }

        if ("운동·식단 균형형에 가까워요".equals(balancePatternInsight.getTitle())) {
            return String.format("%s %s + 기록 균형형 패턴이에요", activitySummary, timeSummary);
        }

        return String.format("%s %s 루틴이 눈에 띄어요", activitySummary, timeSummary);
    }

    private TimePatternInsight analyzeTimePattern(List<ExerciseLog> exerciseLogs) {
        Map<TimeBucket, Integer> counts = new EnumMap<>(TimeBucket.class);
        for (TimeBucket timeBucket : TimeBucket.values()) {
            counts.put(timeBucket, 0);
        }

        int total = 0;
        for (ExerciseLog exerciseLog : exerciseLogs) {
            LocalDateTime createdAt = exerciseLog.getCreatedAt();
            if (createdAt == null) {
                continue;
            }
            TimeBucket timeBucket = TimeBucket.fromHour(createdAt.getHour());
            counts.put(timeBucket, counts.get(timeBucket) + 1);
            total++;
        }

        if (total == 0) {
            return new TimePatternInsight(
                    LifestyleInsightDto.builder()
                            .title("운동 기록 시간대는 아직 유동적이에요")
                            .description("기록 시각 정보가 충분하지 않아 특정 시간대 루틴은 더 지켜봐야 해요.")
                            .tone("muted")
                            .build(),
                    "자유형",
                    0
            );
        }

        Map.Entry<TimeBucket, Integer> topEntry = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();

        TimeBucket topBucket = topEntry.getKey();
        int percent = (int) Math.round((topEntry.getValue() * 100.0) / total);
        String description = percent >= 45
                ? String.format("운동 기록의 %d%%가 %s에 몰려 있어요. 이 시간대에 루틴이 가장 잘 맞는 편이에요.", percent, topBucket.getRangeLabel())
                : String.format("기록 시각이 한쪽으로 강하게 치우치진 않지만, 가장 자주 남긴 시간대는 %s예요.", topBucket.getRangeLabel());

        return new TimePatternInsight(
                LifestyleInsightDto.builder()
                        .title(topBucket.getTitle())
                        .description(description)
                        .tone(topBucket.getTone())
                        .build(),
                topBucket.getSummaryKeyword(),
                percent
        );
    }

    private ActivityPatternInsight analyzeActivityPattern(List<ExerciseLog> exerciseLogs, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, BigDecimal> dailyBurnedMap = new LinkedHashMap<>();
        for (LocalDate cursor = startDate; !cursor.isAfter(endDate); cursor = cursor.plusDays(1)) {
            dailyBurnedMap.put(cursor, BigDecimal.ZERO);
        }

        for (ExerciseLog exerciseLog : exerciseLogs) {
            LocalDate exerciseDate = exerciseLog.getExerciseDate();
            if (exerciseDate == null || !dailyBurnedMap.containsKey(exerciseDate)) {
                continue;
            }
            BigDecimal currentValue = dailyBurnedMap.get(exerciseDate);
            dailyBurnedMap.put(exerciseDate, currentValue.add(nullToZero(exerciseLog.getBurnedKcal())));
        }

        double weekendTotal = 0.0;
        double weekdayTotal = 0.0;
        int weekendDays = 0;
        int weekdayDays = 0;

        for (Map.Entry<LocalDate, BigDecimal> entry : dailyBurnedMap.entrySet()) {
            double burned = entry.getValue().doubleValue();
            if (isWeekend(entry.getKey().getDayOfWeek())) {
                weekendTotal += burned;
                weekendDays++;
            } else {
                weekdayTotal += burned;
                weekdayDays++;
            }
        }

        double weekendAverage = weekendDays > 0 ? weekendTotal / weekendDays : 0.0;
        double weekdayAverage = weekdayDays > 0 ? weekdayTotal / weekdayDays : 0.0;

        if (weekendAverage >= Math.max(weekdayAverage * 1.45, 80.0)) {
            return new ActivityPatternInsight(
                    LifestyleInsightDto.builder()
                            .title("주말 몰아 운동형")
                            .description(String.format("주말 하루 평균 소모량이 %skcal로 평일보다 %s배 높아요. 주말에 활동량이 확 올라가는 타입이에요.", formatNumber(weekendAverage), formatDecimal(safeRatio(weekendAverage, weekdayAverage))))
                            .tone("weekend")
                            .build(),
                    "주말 몰아형"
            );
        }

        if (weekdayAverage >= Math.max(weekendAverage * 1.25, 80.0)) {
            return new ActivityPatternInsight(
                    LifestyleInsightDto.builder()
                            .title("평일 루틴형")
                            .description(String.format("평일 하루 평균 소모량이 %skcal로 주말보다 높아요. 일상 루틴 안에서 운동을 챙기는 편이에요.", formatNumber(weekdayAverage)))
                            .tone("accent")
                            .build(),
                    "평일 루틴형"
            );
        }

        return new ActivityPatternInsight(
                LifestyleInsightDto.builder()
                        .title("요일별 운동 편차가 크지 않아요")
                        .description("주중과 주말 활동량 차이가 크지 않아 비교적 고르게 움직이는 패턴이에요.")
                        .tone("balance")
                        .build(),
                "고른 분산형"
        );
    }

    private LifestyleInsightDto analyzeMealPattern(List<MealLog> mealLogs, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Set<String>> mainMealsByDate = new HashMap<>();
        for (MealLog mealLog : mealLogs) {
            if (mealLog.getMealDate() == null || !MAIN_MEAL_TYPES.contains(mealLog.getMealType())) {
                continue;
            }
            mainMealsByDate.computeIfAbsent(mealLog.getMealDate(), key -> new HashSet<>()).add(mealLog.getMealType());
        }

        Map<DayOfWeek, Integer> weekdayOccurrences = new EnumMap<>(DayOfWeek.class);
        Map<DayOfWeek, Integer> weekdayMissingSlots = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            weekdayOccurrences.put(dayOfWeek, 0);
            weekdayMissingSlots.put(dayOfWeek, 0);
        }

        for (LocalDate cursor = startDate; !cursor.isAfter(endDate); cursor = cursor.plusDays(1)) {
            DayOfWeek dayOfWeek = cursor.getDayOfWeek();
            weekdayOccurrences.put(dayOfWeek, weekdayOccurrences.get(dayOfWeek) + 1);
            int recordedMeals = mainMealsByDate.getOrDefault(cursor, Set.of()).size();
            weekdayMissingSlots.put(dayOfWeek, weekdayMissingSlots.get(dayOfWeek) + Math.max(0, 3 - recordedMeals));
        }

        DayOfWeek weakestDay = DayOfWeek.MONDAY;
        double highestMissingRatio = -1.0;
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            int occurrences = weekdayOccurrences.get(dayOfWeek);
            if (occurrences == 0) {
                continue;
            }
            double missingRatio = weekdayMissingSlots.get(dayOfWeek) / (occurrences * 3.0);
            if (missingRatio > highestMissingRatio) {
                highestMissingRatio = missingRatio;
                weakestDay = dayOfWeek;
            }
        }

        if (highestMissingRatio < 0.2) {
            return LifestyleInsightDto.builder()
                    .title("식단 기록이 비교적 안정적이에요")
                    .description("요일별 편차가 크지 않아 아침·점심·저녁 기록이 비교적 고르게 남아 있어요.")
                    .tone("balance")
                    .build();
        }

        return LifestyleInsightDto.builder()
                .title(String.format("%s 식단 기록이 자주 비어요", toKoreanDayLabel(weakestDay)))
                .description(String.format("최근 4주 기준 %s은 주요 식사 기록 누락 비율이 %d%%였어요. 이 요일만 조금 더 챙기면 패턴이 훨씬 안정돼요.", toKoreanDayLabel(weakestDay), (int) Math.round(highestMissingRatio * 100.0)))
                .tone("caution")
                .build();
    }

    private LifestyleInsightDto analyzeBalancePattern(List<ExerciseLog> exerciseLogs,
                                                      List<MealLog> mealLogs,
                                                      LocalDate startDate,
                                                      LocalDate endDate) {
        int exerciseDays = distinctExerciseDays(exerciseLogs).size();

        Map<LocalDate, Set<String>> mainMealsByDate = new HashMap<>();
        for (MealLog mealLog : mealLogs) {
            if (mealLog.getMealDate() == null || !MAIN_MEAL_TYPES.contains(mealLog.getMealType())) {
                continue;
            }
            mainMealsByDate.computeIfAbsent(mealLog.getMealDate(), key -> new HashSet<>()).add(mealLog.getMealType());
        }

        int totalRecordedMealSlots = 0;
        int wellTrackedMealDays = 0;
        for (LocalDate cursor = startDate; !cursor.isAfter(endDate); cursor = cursor.plusDays(1)) {
            int recordedCount = mainMealsByDate.getOrDefault(cursor, Set.of()).size();
            totalRecordedMealSlots += recordedCount;
            if (recordedCount >= 2) {
                wellTrackedMealDays++;
            }
        }

        double mealCoverageRatio = totalRecordedMealSlots / (ANALYSIS_DAYS * 3.0);

        if (exerciseDays >= 8 && mealCoverageRatio < 0.38) {
            return LifestyleInsightDto.builder()
                    .title("운동은 꾸준한데 식단 기록은 약한 편")
                    .description(String.format("최근 28일 동안 운동은 %d일 기록했지만, 주요 식사 기록 채움률은 %d%%예요. 운동만큼 식단 기록도 같이 쌓이면 분석이 더 선명해져요.", exerciseDays, (int) Math.round(mealCoverageRatio * 100.0)))
                    .tone("caution")
                    .build();
        }

        if (wellTrackedMealDays >= 10 && exerciseDays <= 4) {
            return LifestyleInsightDto.builder()
                    .title("식단은 꼼꼼하고 운동 기록은 가벼운 편")
                    .description(String.format("식단이 잘 기록된 날은 %d일인데, 운동 기록일은 %d일이에요. 운동 루틴만 조금 더 붙이면 균형이 좋아져요.", wellTrackedMealDays, exerciseDays))
                    .tone("accent")
                    .build();
        }

        if (exerciseDays >= 8 && mealCoverageRatio >= 0.5) {
            return LifestyleInsightDto.builder()
                    .title("운동·식단 균형형에 가까워요")
                    .description(String.format("운동 기록일 %d일, 주요 식사 기록 채움률 %d%%로 두 영역이 비교적 함께 유지되고 있어요.", exerciseDays, (int) Math.round(mealCoverageRatio * 100.0)))
                    .tone("balance")
                    .build();
        }

        return LifestyleInsightDto.builder()
                .title("기록 습관을 만드는 단계예요")
                .description("운동과 식단 중 한쪽으로 크게 치우치진 않았어요. 자주 기록하는 루틴을 하나 정하면 패턴이 더 뚜렷해져요.")
                .tone("muted")
                .build();
    }

    private Set<LocalDate> distinctExerciseDays(List<ExerciseLog> exerciseLogs) {
        Set<LocalDate> exerciseDays = new HashSet<>();
        exerciseLogs.stream()
                .map(ExerciseLog::getExerciseDate)
                .filter(java.util.Objects::nonNull)
                .forEach(exerciseDays::add);
        return exerciseDays;
    }

    private Set<LocalDate> distinctMealDays(List<MealLog> mealLogs) {
        Set<LocalDate> mealDays = new HashSet<>();
        mealLogs.stream()
                .map(MealLog::getMealDate)
                .filter(java.util.Objects::nonNull)
                .forEach(mealDays::add);
        return mealDays;
    }

    private boolean isWeekend(DayOfWeek dayOfWeek) {
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatDecimal(double value) {
        return String.format(Locale.KOREA, "%.1f", value);
    }

    private String formatNumber(double value) {
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private double safeRatio(double numerator, double denominator) {
        if (denominator <= 0.0) {
            return numerator > 0.0 ? numerator : 0.0;
        }
        return numerator / denominator;
    }

    private String toKoreanDayLabel(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "월요일";
            case TUESDAY -> "화요일";
            case WEDNESDAY -> "수요일";
            case THURSDAY -> "목요일";
            case FRIDAY -> "금요일";
            case SATURDAY -> "토요일";
            case SUNDAY -> "일요일";
        };
    }

    private enum TimeBucket {
        MORNING("아침형 기록 패턴", "오전 5시~10시", "accent", "아침형"),
        DAYTIME("낮 시간 기록형", "오전 11시~오후 4시", "balance", "낮 집중형"),
        EVENING("저녁 집중형", "오후 5시~8시", "weekend", "저녁형"),
        NIGHT("야행성 기록 패턴", "오후 9시~새벽 4시", "caution", "야행성");

        private final String title;
        private final String rangeLabel;
        private final String tone;
        private final String summaryKeyword;

        TimeBucket(String title, String rangeLabel, String tone, String summaryKeyword) {
            this.title = title;
            this.rangeLabel = rangeLabel;
            this.tone = tone;
            this.summaryKeyword = summaryKeyword;
        }

        public String getTitle() {
            return title;
        }

        public String getRangeLabel() {
            return rangeLabel;
        }

        public String getTone() {
            return tone;
        }

        public String getSummaryKeyword() {
            return summaryKeyword;
        }

        private static TimeBucket fromHour(int hour) {
            if (hour >= 5 && hour <= 10) {
                return MORNING;
            }
            if (hour >= 11 && hour <= 16) {
                return DAYTIME;
            }
            if (hour >= 17 && hour <= 20) {
                return EVENING;
            }
            return NIGHT;
        }
    }

    private record TimePatternInsight(LifestyleInsightDto insight, String summaryKeyword, int percent) {
    }

    private record ActivityPatternInsight(LifestyleInsightDto insight, String summaryKeyword) {
    }
}

