package com.ownlife.repository;

import com.ownlife.entity.MealLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MealLogRepository extends JpaRepository<MealLog, Long> {

    List<MealLog> findByMemberIdAndMealDate(Long memberId, LocalDate mealDate);

    List<MealLog> findByMemberIdAndMealDateBetween(Long memberId, LocalDate startDate, LocalDate endDate);

    void deleteByMemberIdAndMealDateAndMealType(Long memberId, LocalDate mealDate, String mealType);

    @Query("""
            select distinct m.mealDate
            from MealLog m
            where m.memberId = :memberId
              and m.mealDate between :startDate and :endDate
            order by m.mealDate desc
            """)
    List<LocalDate> findDistinctMealDatesBetween(@Param("memberId") Long memberId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

}