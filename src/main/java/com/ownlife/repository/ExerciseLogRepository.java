package com.ownlife.repository;

import com.ownlife.entity.ExerciseLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExerciseLogRepository extends JpaRepository<ExerciseLog, Long> {

    List<ExerciseLog> findByMember_MemberIdAndExerciseDateOrderByCreatedAtDesc(Long memberId, LocalDate exerciseDate);

    List<ExerciseLog> findByMember_MemberIdAndExerciseDateBetweenOrderByExerciseDateAscCreatedAtAsc(
            Long memberId,
            LocalDate startDate,
            LocalDate endDate
    );

    @Query("""
            select coalesce(sum(e.burnedKcal), 0)
            from ExerciseLog e
            where e.member.memberId = :memberId
              and e.exerciseDate = :exerciseDate
            """)
    BigDecimal sumBurnedKcalByMemberAndDate(@Param("memberId") Long memberId,
                                            @Param("exerciseDate") LocalDate exerciseDate);

    @Query("""
            select coalesce(sum(e.durationMin), 0)
            from ExerciseLog e
            where e.member.memberId = :memberId
              and e.exerciseDate = :exerciseDate
            """)
    Integer sumDurationByMemberAndDate(@Param("memberId") Long memberId,
                                       @Param("exerciseDate") LocalDate exerciseDate);

    @Query("""
            select coalesce(sum(e.burnedKcal), 0)
            from ExerciseLog e
            where e.member.memberId = :memberId
              and e.exerciseDate between :startDate and :endDate
            """)
    BigDecimal sumBurnedKcalByMemberAndDateBetween(@Param("memberId") Long memberId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);
}
