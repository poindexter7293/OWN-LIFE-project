package com.ownlife.repository;

import com.ownlife.entity.Food;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodRepository extends JpaRepository<Food, Long> {

    Optional<Food> findByFoodNameAndCaloriesKcalAndCarbGAndProteinGAndFatGAndBaseAmountG(
            String foodName,
            Double caloriesKcal,
            Double carbG,
            Double proteinG,
            Double fatG,
            Double baseAmountG
    );

}