package com.ownlife.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "meal_log")
@Getter
@Setter
@NoArgsConstructor
public class MealLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meal_log_id")
    private Long mealLogId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @Column(name = "meal_type", nullable = false, length = 20)
    private String mealType;

    @Column(name = "food_id", nullable = true)
    private Long foodId;

    @Column(name = "food_name_snapshot", nullable = false, length = 100)
    private String foodNameSnapshot;

    @Column(name = "intake_g", nullable = false)
    private Double intakeG;

    @Column(name = "calories_kcal", nullable = false)
    private Double caloriesKcal;

    @Column(name = "carb_g", nullable = false)
    private Double carbG;

    @Column(name = "protein_g", nullable = false)
    private Double proteinG;

    @Column(name = "fat_g", nullable = false)
    private Double fatG;
}