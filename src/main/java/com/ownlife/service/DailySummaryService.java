package com.ownlife.service;

import com.ownlife.entity.DailySummary;
import com.ownlife.entity.Member;
import com.ownlife.entity.MemberGoalHistory;
import com.ownlife.entity.WeightLog;
import com.ownlife.repository.DailySummaryRepository;
import com.ownlife.repository.ExerciseLogRepository;
import com.ownlife.repository.MealLogRepository;
import com.ownlife.repository.MemberGoalHistoryRepository;
import com.ownlife.repository.MemberRepository;
import com.ownlife.repository.WeightLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DailySummaryService {

    private final MemberRepository memberRepository;
    private final MealLogRepository mealLogRepository;
    private final ExerciseLogRepository exerciseLogRepository;
    private final WeightLogRepository weightLogRepository;
    private final MemberGoalHistoryRepository memberGoalHistoryRepository;
    private final DailySummaryRepository dailySummaryRepository;

    /**
     * 매일 새벽 1시 10분에 전날 데이터를 사용자별로 집계
     */
    @Scheduled(cron = "0 10 1 * * *", zone = "Asia/Seoul")
    public void summarizeYesterdayForAllMembers() {
        LocalDate targetDate = LocalDate.now().minusDays(1);
        summarizeAllMembers(targetDate);
    }

    public void summarizeAllMembers(LocalDate summaryDate) {
        List<Member> members = memberRepository.findAll();

        for (Member member : members) {
            if (member.getStatus() == Member.Status.DELETED) {
                continue;
            }
            summarizeMemberDaily(member.getMemberId(), summaryDate);
        }

        log.info("일일 집계 완료 - summaryDate={}", summaryDate);
    }

    public void summarizeMemberDaily(Long memberId, LocalDate summaryDate) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다. memberId=" + memberId));

        DailySummary dailySummary = dailySummaryRepository
                .findByMember_MemberIdAndSummaryDate(memberId, summaryDate)
                .orElseGet(DailySummary::new);

        dailySummary.setMember(member);
        dailySummary.setSummaryDate(summaryDate);

        dailySummary.setWeightKg(resolveWeight(memberId, summaryDate));

        dailySummary.setTotalEatKcal(scale(decimalOf(
                mealLogRepository.sumCaloriesByMemberAndDate(memberId, summaryDate)
        )));
        dailySummary.setTotalCarbG(scale(decimalOf(
                mealLogRepository.sumCarbByMemberAndDate(memberId, summaryDate)
        )));
        dailySummary.setTotalProteinG(scale(decimalOf(
                mealLogRepository.sumProteinByMemberAndDate(memberId, summaryDate)
        )));
        dailySummary.setTotalFatG(scale(decimalOf(
                mealLogRepository.sumFatByMemberAndDate(memberId, summaryDate)
        )));

        dailySummary.setTotalBurnedKcal(scale(
                nvl(exerciseLogRepository.sumBurnedKcalByMemberAndDate(memberId, summaryDate))
        ));

        applyGoalSnapshot(dailySummary, member, memberId, summaryDate);

        dailySummaryRepository.save(dailySummary);

        log.info("일일 요약 저장 완료 - memberId={}, summaryDate={}", memberId, summaryDate);
    }

    private BigDecimal resolveWeight(Long memberId, LocalDate summaryDate) {
        return weightLogRepository
                .findFirstByMember_MemberIdAndLogDateLessThanEqualOrderByLogDateDesc(memberId, summaryDate)
                .map(WeightLog::getWeightKg)
                .map(this::scale)
                .orElse(null);
    }

    private void applyGoalSnapshot(DailySummary dailySummary,
                                   Member member,
                                   Long memberId,
                                   LocalDate summaryDate) {

        LocalDateTime endOfDay = summaryDate.atTime(LocalTime.MAX);

        MemberGoalHistory goalHistory = memberGoalHistoryRepository
                .findFirstByMember_MemberIdAndChangedAtLessThanEqualOrderByChangedAtDesc(memberId, endOfDay)
                .orElse(null);

        if (goalHistory != null) {
            dailySummary.setGoalEatKcal(goalHistory.getGoalEatKcal());
            dailySummary.setGoalBurnedKcal(goalHistory.getGoalBurnedKcal());
            dailySummary.setGoalWeight(scale(goalHistory.getGoalWeight()));
            return;
        }

        dailySummary.setGoalEatKcal(member.getGoalEatKcal());
        dailySummary.setGoalBurnedKcal(member.getGoalBurnedKcal());
        dailySummary.setGoalWeight(scale(member.getGoalWeight()));
    }

    private BigDecimal decimalOf(Double value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}