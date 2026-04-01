package com.ownlife.repository;

import com.ownlife.entity.ExerciseType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExerciseTypeRepository extends JpaRepository<ExerciseType, Long> {

    Optional<ExerciseType> findFirstByCategoryAndIsActiveTrue(ExerciseType.Category category);
}