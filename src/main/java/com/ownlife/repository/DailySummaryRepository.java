package com.ownlife.repository;

import com.ownlife.entity.DailySummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailySummaryRepository extends JpaRepository<DailySummary, Long> {

    Optional<DailySummary> findByMember_MemberIdAndSummaryDate(Long memberId, LocalDate summaryDate);
}