package com.ownlife.repository;

import com.ownlife.entity.MemberGoalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberGoalHistoryRepository extends JpaRepository<MemberGoalHistory, Long> {

    List<MemberGoalHistory> findByMember_MemberIdAndChangedAtLessThanEqualOrderByChangedAtAsc(
            Long memberId,
            LocalDateTime changedAt
    );

    Optional<MemberGoalHistory> findFirstByMember_MemberIdAndChangedAtLessThanEqualOrderByChangedAtDesc(
            Long memberId,
            LocalDateTime changedAt
    );


}