package com.ownlife.service;

import com.ownlife.entity.ExerciseLog;
import com.ownlife.entity.ExerciseType;
import com.ownlife.entity.Member;
import com.ownlife.repository.ExerciseLogRepository;
import com.ownlife.repository.ExerciseTypeRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExerciseService {

    private static final DateTimeFormatter LABEL_FORMATTER = DateTimeFormatter.ofPattern("M/d", Locale.KOREA);

    private final ExerciseLogRepository exerciseLogRepository;
    private final ExerciseTypeRepository exerciseTypeRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public ExercisePageData getPageData(Long memberId, LocalDate baseDate) {
        LocalDate selectedDate = baseDate == null ? LocalDate.now() : baseDate;
        LocalDate weekStart = selectedDate.minusDays(6);

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
                dailyMap.put(exerciseDate,
                        dailyMap.get(exerciseDate).add(safeDecimal(log.getBurnedKcal())));
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

        return ExercisePageData.builder()
                .selectedDate(selectedDate)
                .todayBurnedKcal(todayBurned.setScale(0, RoundingMode.HALF_UP).intValue())
                .todayDurationMin(todayDuration)
                .weekBurnedKcal(weekBurned.setScale(0, RoundingMode.HALF_UP).intValue())
                .weekLabels(labels)
                .weekValues(values)
                .todayExercises(items)
                .build();
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
        result.addAll(getQuickOptions(ExerciseType.Category.ROUTE));
        result.addAll(getQuickOptions(ExerciseType.Category.TIME));
        return result;
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

    @Getter
    @Builder
    public static class ExercisePageData {
        private LocalDate selectedDate;
        private int todayBurnedKcal;
        private int todayDurationMin;
        private int weekBurnedKcal;
        private List<String> weekLabels;
        private List<Integer> weekValues;
        private List<ExerciseItem> todayExercises;
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