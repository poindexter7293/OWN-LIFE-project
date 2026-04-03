package com.ownlife.service;

import com.ownlife.entity.ExerciseLog;
import com.ownlife.entity.ExerciseType;
import com.ownlife.entity.Member;
import com.ownlife.entity.MemberGoalHistory;
import com.ownlife.repository.ExerciseLogRepository;
import com.ownlife.repository.ExerciseTypeRepository;
import com.ownlife.repository.MemberGoalHistoryRepository;
import com.ownlife.repository.MemberRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExerciseService {

    private static final DateTimeFormatter LABEL_FORMATTER = DateTimeFormatter.ofPattern("M/d", Locale.KOREA);
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yy.MM", Locale.KOREA);

    private final ExerciseLogRepository exerciseLogRepository;
    private final ExerciseTypeRepository exerciseTypeRepository;
    private final MemberRepository memberRepository;
    private final MemberGoalHistoryRepository memberGoalHistoryRepository;

    @Transactional(readOnly = true)
    public ExercisePageData getPageData(Long memberId, LocalDate baseDate) {
        LocalDate selectedDate = baseDate == null ? LocalDate.now() : baseDate;
        LocalDate weekStart = selectedDate.minusDays(6);

        Member member = getMember(memberId);

        List<ExerciseLog> todayLogs = exerciseLogRepository
                .findByMember_MemberIdAndExerciseDateOrderByCreatedAtDesc(memberId, selectedDate);

        List<ExerciseLog> weekLogs = exerciseLogRepository
                .findByMember_MemberIdAndExerciseDateBetweenOrderByExerciseDateAscCreatedAtAsc(memberId, weekStart, selectedDate);

        BigDecimal todayBurned = safeDecimal(exerciseLogRepository.sumBurnedKcalByMemberAndDate(memberId, selectedDate));
        Integer todayDuration = safeInteger(exerciseLogRepository.sumDurationByMemberAndDate(memberId, selectedDate));
        BigDecimal weekBurned = safeDecimal(exerciseLogRepository.sumBurnedKcalByMemberAndDateBetween(memberId, weekStart, selectedDate));

        Map<LocalDate, BigDecimal> dailyMap = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            dailyMap.put(weekStart.plusDays(i), BigDecimal.ZERO);
        }

        for (ExerciseLog log : weekLogs) {
            LocalDate exerciseDate = log.getExerciseDate();
            if (exerciseDate != null && dailyMap.containsKey(exerciseDate)) {
                dailyMap.put(exerciseDate, dailyMap.get(exerciseDate).add(safeDecimal(log.getBurnedKcal())));
            }
        }

        List<String> labels = dailyMap.keySet().stream()
                .map(LABEL_FORMATTER::format)
                .toList();

        List<Integer> values = dailyMap.values().stream()
                .map(value -> value.setScale(0, RoundingMode.HALF_UP).intValue())
                .toList();

        List<ExerciseItem> items = todayLogs.stream()
                .sorted(Comparator
                        .comparing(ExerciseLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ExerciseLog::getExerciseLogId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toItem)
                .toList();

        int todayBurnedKcal = todayBurned.setScale(0, RoundingMode.HALF_UP).intValue();
        int weekBurnedKcal = weekBurned.setScale(0, RoundingMode.HALF_UP).intValue();
        int goalBurnedKcal = safeInteger(member.getGoalBurnedKcal());

        int goalRemainingKcal = Math.max(goalBurnedKcal - todayBurnedKcal, 0);
        int goalAchievementRate = goalBurnedKcal > 0
                ? (int) Math.round((todayBurnedKcal * 100.0) / goalBurnedKcal)
                : 0;

        return ExercisePageData.builder()
                .selectedDate(selectedDate)
                .todayBurnedKcal(todayBurnedKcal)
                .todayDurationMin(todayDuration)
                .weekBurnedKcal(weekBurnedKcal)
                .goalBurnedKcal(goalBurnedKcal)
                .goalRemainingKcal(goalRemainingKcal)
                .goalAchievementRate(goalAchievementRate)
                .weekLabels(labels)
                .weekValues(values)
                .todayExercises(items)
                .build();
    }

    @Transactional(readOnly = true)
    public ExerciseChartData getChartData(Long memberId, LocalDate baseDate, ChartPeriod period) {
        LocalDate selectedDate = baseDate == null ? LocalDate.now() : baseDate;
        Member member = getMember(memberId);

        if (period == null) {
            period = ChartPeriod.WEEK;
        }

        return switch (period) {
            case WEEK -> buildDailyChart(member, selectedDate.minusDays(6), selectedDate, period);
            case MONTH -> buildDailyChart(member, selectedDate.minusDays(29), selectedDate, period);
            case YEAR -> buildMonthlyChart(member, YearMonth.from(selectedDate).minusMonths(11), YearMonth.from(selectedDate), period);
        };
    }

    private ExerciseChartData buildDailyChart(Member member, LocalDate startDate, LocalDate endDate, ChartPeriod period) {
        Long memberId = member.getMemberId();

        List<ExerciseLog> logs = exerciseLogRepository
                .findByMember_MemberIdAndExerciseDateBetweenOrderByExerciseDateAscCreatedAtAsc(memberId, startDate, endDate);

        List<MemberGoalHistory> histories = memberGoalHistoryRepository
                .findByMember_MemberIdAndChangedAtLessThanEqualOrderByChangedAtAsc(memberId, endDate.atTime(23, 59, 59));

        Map<LocalDate, Integer> burnedMap = new LinkedHashMap<>();
        Map<LocalDate, Integer> goalMap = new LinkedHashMap<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            burnedMap.put(date, 0);
            goalMap.put(date, resolveGoalBurnedKcalForDate(member, histories, date));
        }

        for (ExerciseLog log : logs) {
            LocalDate exerciseDate = log.getExerciseDate();
            if (exerciseDate != null && burnedMap.containsKey(exerciseDate)) {
                int current = burnedMap.get(exerciseDate);
                int add = safeDecimal(log.getBurnedKcal()).setScale(0, RoundingMode.HALF_UP).intValue();
                burnedMap.put(exerciseDate, current + add);
            }
        }

        List<String> labels = burnedMap.keySet().stream()
                .map(LABEL_FORMATTER::format)
                .toList();

        List<Integer> burnedValues = new ArrayList<>(burnedMap.values());
        List<Integer> goalValues = new ArrayList<>(goalMap.values());

        return ExerciseChartData.builder()
                .period(period.name())
                .labels(labels)
                .burnedValues(burnedValues)
                .goalValues(goalValues)
                .build();
    }

    private ExerciseChartData buildMonthlyChart(Member member, YearMonth startMonth, YearMonth endMonth, ChartPeriod period) {
        Long memberId = member.getMemberId();

        LocalDate startDate = startMonth.atDay(1);
        LocalDate endDate = endMonth.atEndOfMonth();

        List<ExerciseLog> logs = exerciseLogRepository
                .findByMember_MemberIdAndExerciseDateBetweenOrderByExerciseDateAscCreatedAtAsc(memberId, startDate, endDate);

        List<MemberGoalHistory> histories = memberGoalHistoryRepository
                .findByMember_MemberIdAndChangedAtLessThanEqualOrderByChangedAtAsc(memberId, endDate.atTime(23, 59, 59));

        Map<YearMonth, Integer> monthlyTotalMap = new LinkedHashMap<>();
        Map<YearMonth, Integer> burnedMap = new LinkedHashMap<>();
        Map<YearMonth, Integer> goalMap = new LinkedHashMap<>();

        for (YearMonth ym = startMonth; !ym.isAfter(endMonth); ym = ym.plusMonths(1)) {
            monthlyTotalMap.put(ym, 0);
            burnedMap.put(ym, 0);
            goalMap.put(ym, resolveGoalBurnedKcalForMonth(member, histories, ym));
        }

        for (ExerciseLog log : logs) {
            if (log.getExerciseDate() == null) {
                continue;
            }

            YearMonth ym = YearMonth.from(log.getExerciseDate());
            if (monthlyTotalMap.containsKey(ym)) {
                int current = monthlyTotalMap.get(ym);
                int add = safeDecimal(log.getBurnedKcal()).setScale(0, RoundingMode.HALF_UP).intValue();
                monthlyTotalMap.put(ym, current + add);
            }
        }

        for (Map.Entry<YearMonth, Integer> entry : monthlyTotalMap.entrySet()) {
            YearMonth ym = entry.getKey();
            int monthlyTotal = entry.getValue();

            long activeDays = logs.stream()
                    .filter(log -> log.getExerciseDate() != null)
                    .filter(log -> YearMonth.from(log.getExerciseDate()).equals(ym))
                    .map(ExerciseLog::getExerciseDate)
                    .distinct()
                    .count();

            int averagePerDay = activeDays > 0
                    ? (int) Math.round(monthlyTotal / (double) activeDays)
                    : 0;

            burnedMap.put(ym, averagePerDay);
        }

        List<String> labels = burnedMap.keySet().stream()
                .map(MONTH_LABEL_FORMATTER::format)
                .toList();

        List<Integer> burnedValues = new ArrayList<>(burnedMap.values());
        List<Integer> goalValues = new ArrayList<>(goalMap.values());

        return ExerciseChartData.builder()
                .period(period.name())
                .labels(labels)
                .burnedValues(burnedValues)
                .goalValues(goalValues)
                .build();
    }

    private int resolveGoalBurnedKcalForDate(Member member, List<MemberGoalHistory> histories, LocalDate date) {
        LocalDateTime target = date.atTime(23, 59, 59);

        Integer latestGoal = null;
        for (MemberGoalHistory history : histories) {
            if (history.getChangedAt() == null || history.getChangedAt().isAfter(target)) {
                break;
            }
            if (history.getGoalBurnedKcal() != null) {
                latestGoal = history.getGoalBurnedKcal();
            }
        }

        if (latestGoal != null) {
            return latestGoal;
        }

        if (!histories.isEmpty() && histories.get(0).getGoalBurnedKcal() != null) {
            return histories.get(0).getGoalBurnedKcal();
        }

        return safeInteger(member.getGoalBurnedKcal());
    }

    private int resolveGoalBurnedKcalForMonth(Member member, List<MemberGoalHistory> histories, YearMonth month) {
        LocalDateTime target = month.atEndOfMonth().atTime(23, 59, 59);

        Integer latestGoal = null;
        for (MemberGoalHistory history : histories) {
            if (history.getChangedAt() == null || history.getChangedAt().isAfter(target)) {
                break;
            }
            if (history.getGoalBurnedKcal() != null) {
                latestGoal = history.getGoalBurnedKcal();
            }
        }

        if (latestGoal != null) {
            return latestGoal;
        }

        if (!histories.isEmpty() && histories.get(0).getGoalBurnedKcal() != null) {
            return histories.get(0).getGoalBurnedKcal();
        }

        return safeInteger(member.getGoalBurnedKcal());
    }

    public void addDirectExercise(Long memberId, LocalDate exerciseDate, String exerciseName, Integer burnedKcal) {
        if (!StringUtils.hasText(exerciseName)) {
            throw new IllegalArgumentException("운동 이름을 입력해 주세요.");
        }
        if (burnedKcal == null || burnedKcal <= 0) {
            throw new IllegalArgumentException("소모 칼로리는 1 이상이어야 합니다.");
        }

        Member member = getMember(memberId);
        ExerciseType directInputType = getDirectInputType();

        ExerciseLog log = new ExerciseLog();
        log.setMember(member);
        log.setExerciseType(directInputType);
        log.setExerciseDate(defaultDate(exerciseDate));
        log.setDurationMin(null);
        log.setSetsCount(null);
        log.setRepsCount(null);
        log.setDistanceKm(null);
        log.setBurnedKcal(BigDecimal.valueOf(burnedKcal));
        log.setMemo(exerciseName.trim());

        exerciseLogRepository.save(log);
    }

    public void addQuickCountExercise(Long memberId, LocalDate exerciseDate, Long exerciseTypeId, Integer setsCount, Integer repsCount) {
        if (exerciseTypeId == null) {
            throw new IllegalArgumentException("운동 종류를 선택해 주세요.");
        }
        if (setsCount == null || setsCount <= 0) {
            throw new IllegalArgumentException("세트 수는 1 이상이어야 합니다.");
        }
        if (repsCount == null || repsCount <= 0) {
            throw new IllegalArgumentException("횟수는 1 이상이어야 합니다.");
        }

        Member member = getMember(memberId);
        ExerciseType exerciseType = getExerciseType(exerciseTypeId);

        if (exerciseType.getCategory() != ExerciseType.Category.COUNT_SET) {
            throw new IllegalArgumentException("세트형 운동만 추가할 수 있습니다.");
        }
        if (exerciseType.getKcalPerRep() == null) {
            throw new IllegalArgumentException("회당 칼로리 정보가 없습니다.");
        }

        BigDecimal burnedKcal = exerciseType.getKcalPerRep()
                .multiply(BigDecimal.valueOf(setsCount.longValue()))
                .multiply(BigDecimal.valueOf(repsCount.longValue()))
                .setScale(2, RoundingMode.HALF_UP);

        ExerciseLog log = new ExerciseLog();
        log.setMember(member);
        log.setExerciseType(exerciseType);
        log.setExerciseDate(defaultDate(exerciseDate));
        log.setSetsCount(setsCount);
        log.setRepsCount(repsCount);
        log.setDurationMin(null);
        log.setDistanceKm(null);
        log.setBurnedKcal(burnedKcal);
        log.setMemo(null);

        exerciseLogRepository.save(log);
    }

    public void addQuickTimeExercise(Long memberId, LocalDate exerciseDate, Long exerciseTypeId, Integer durationMin) {
        if (exerciseTypeId == null) {
            throw new IllegalArgumentException("운동 종류를 선택해 주세요.");
        }
        if (durationMin == null || durationMin <= 0) {
            throw new IllegalArgumentException("운동 시간은 1분 이상이어야 합니다.");
        }

        Member member = getMember(memberId);
        ExerciseType exerciseType = getExerciseType(exerciseTypeId);

        if (exerciseType.getCategory() != ExerciseType.Category.TIME
                && exerciseType.getCategory() != ExerciseType.Category.ROUTE) {
            throw new IllegalArgumentException("시간형 운동만 추가할 수 있습니다.");
        }
        if (exerciseType.getKcalPerMin() == null) {
            throw new IllegalArgumentException("분당 칼로리 정보가 없습니다.");
        }

        BigDecimal burnedKcal = exerciseType.getKcalPerMin()
                .multiply(BigDecimal.valueOf(durationMin.longValue()))
                .setScale(2, RoundingMode.HALF_UP);

        ExerciseLog log = new ExerciseLog();
        log.setMember(member);
        log.setExerciseType(exerciseType);
        log.setExerciseDate(defaultDate(exerciseDate));
        log.setDurationMin(durationMin);
        log.setSetsCount(null);
        log.setRepsCount(null);
        log.setDistanceKm(null);
        log.setBurnedKcal(burnedKcal);
        log.setMemo(null);

        exerciseLogRepository.save(log);
    }

    public void deleteExercise(Long memberId, Long exerciseLogId) {
        if (exerciseLogId == null) {
            throw new IllegalArgumentException("삭제할 운동 기록이 없습니다.");
        }

        ExerciseLog log = exerciseLogRepository.findById(exerciseLogId)
                .orElseThrow(() -> new IllegalArgumentException("운동 기록을 찾을 수 없습니다."));

        if (log.getMember() == null || log.getMember().getMemberId() == null
                || !log.getMember().getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 운동 기록만 삭제할 수 있습니다.");
        }

        exerciseLogRepository.delete(log);
    }

    @Transactional(readOnly = true)
    public List<QuickExerciseOption> getCountOptions() {
        return getQuickOptions(ExerciseType.Category.COUNT_SET);
    }

    @Transactional(readOnly = true)
    public List<QuickExerciseOption> getTimeOptions() {
        List<QuickExerciseOption> result = new ArrayList<>();
        result.addAll(getQuickOptions(ExerciseType.Category.TIME));
        result.addAll(getQuickOptions(ExerciseType.Category.ROUTE));
        return result;
    }

    @Transactional(readOnly = true)
    public List<QuickExerciseOption> getRouteOptions() {
        return getQuickOptions(ExerciseType.Category.ROUTE);
    }

    private List<QuickExerciseOption> getQuickOptions(ExerciseType.Category category) {
        return exerciseTypeRepository.findAll().stream()
                .filter(type -> Boolean.TRUE.equals(type.getIsActive()))
                .filter(type -> type.getCategory() == category)
                .filter(type -> type.getExerciseTypeId() != null)
                .sorted(Comparator.comparing(ExerciseType::getExerciseTypeId))
                .map(type -> new QuickExerciseOption(type.getExerciseTypeId(), type.getExerciseName()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private ExerciseItem toItem(ExerciseLog log) {
        ExerciseType exerciseType = log.getExerciseType();

        String displayName = isDirectInput(exerciseType)
                ? defaultText(log.getMemo(), "직접 입력")
                : (exerciseType != null ? defaultText(exerciseType.getExerciseName(), "운동") : "운동");

        String detail = "";
        if (exerciseType != null && exerciseType.getCategory() == ExerciseType.Category.COUNT_SET) {
            detail = safeInteger(log.getSetsCount()) + "세트 · " + safeInteger(log.getRepsCount()) + "회";
        } else if (safeInteger(log.getDurationMin()) > 0) {
            detail = safeInteger(log.getDurationMin()) + "분";
        }

        return ExerciseItem.builder()
                .exerciseLogId(log.getExerciseLogId())
                .exerciseName(displayName)
                .detail(detail)
                .burnedKcal(safeDecimal(log.getBurnedKcal()).setScale(0, RoundingMode.HALF_UP).intValue())
                .build();
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
    }

    private ExerciseType getExerciseType(Long exerciseTypeId) {
        return exerciseTypeRepository.findById(exerciseTypeId)
                .orElseThrow(() -> new IllegalArgumentException("exercise_type 테이블에 필요한 운동 타입이 없습니다. 먼저 기초 데이터를 넣어 주세요."));
    }

    private ExerciseType getDirectInputType() {
        return exerciseTypeRepository.findFirstByCategoryAndIsActiveTrue(ExerciseType.Category.SELF)
                .orElseThrow(() -> new IllegalArgumentException("직접 입력용 운동 타입(SELF)이 없습니다."));
    }

    private boolean isDirectInput(ExerciseType exerciseType) {
        return exerciseType != null && exerciseType.getCategory() == ExerciseType.Category.SELF;
    }

    private LocalDate defaultDate(LocalDate exerciseDate) {
        return exerciseDate == null ? LocalDate.now() : exerciseDate;
    }

    private BigDecimal safeDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer safeInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    public enum ChartPeriod {
        WEEK, MONTH, YEAR
    }

    @Getter
    @Builder
    public static class ExercisePageData {
        private LocalDate selectedDate;
        private int todayBurnedKcal;
        private int todayDurationMin;
        private int weekBurnedKcal;
        private int goalBurnedKcal;
        private int goalRemainingKcal;
        private int goalAchievementRate;
        private List<String> weekLabels;
        private List<Integer> weekValues;
        private List<ExerciseItem> todayExercises;
    }

    @Getter
    @Builder
    public static class ExerciseChartData {
        private String period;
        private List<String> labels;
        private List<Integer> burnedValues;
        private List<Integer> goalValues;
    }

    @Getter
    @Builder
    public static class ExerciseItem {
        private Long exerciseLogId;
        private String exerciseName;
        private String detail;
        private int burnedKcal;
    }

    @Getter
    public static class QuickExerciseOption {
        private final Long exerciseTypeId;
        private final String exerciseName;

        public QuickExerciseOption(Long exerciseTypeId, String exerciseName) {
            this.exerciseTypeId = exerciseTypeId;
            this.exerciseName = exerciseName;
        }
    }
}