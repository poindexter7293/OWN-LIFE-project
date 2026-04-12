package com.ownlife.repository;

import com.ownlife.entity.DailySummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailySummaryRepository extends JpaRepository<DailySummary, Long> {

    Optional<DailySummary> findByMember_MemberIdAndSummaryDate(Long memberId, LocalDate summaryDate);

    List<DailySummary> findByMember_MemberIdAndSummaryDateBetweenOrderBySummaryDateAsc(
            Long memberId,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<DailySummary> findFirstByMember_MemberIdAndSummaryDateLessThanEqualOrderBySummaryDateDesc(
            Long memberId,
            LocalDate summaryDate
    );
}