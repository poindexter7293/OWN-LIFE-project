package com.ownlife.repository;

import com.ownlife.entity.MealLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MealLogRepository extends JpaRepository<MealLog, Long> {

    List<MealLog> findByMemberIdAndMealDate(Long memberId, LocalDate mealDate);

    List<MealLog> findByMemberIdAndMealDateBetween(Long memberId, LocalDate startDate, LocalDate endDate);

}