package com.ownlife.repository;

import com.ownlife.entity.WeightLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeightLogRepository extends JpaRepository<WeightLog, Long> {

    Optional<WeightLog> findFirstByMember_MemberIdOrderByLogDateDesc(Long memberId);

    List<WeightLog> findTop2ByMember_MemberIdOrderByLogDateDesc(Long memberId);

    List<WeightLog> findByMember_MemberIdAndLogDateBetweenOrderByLogDateAsc(
            Long memberId,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<WeightLog> findFirstByMember_MemberIdAndLogDateLessThanEqualOrderByLogDateDesc(
            Long memberId,
            LocalDate logDate
    );
}