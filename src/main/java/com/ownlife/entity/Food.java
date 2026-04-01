package com.ownlife.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "food")
@Getter
@Setter
@NoArgsConstructor
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "food_id")
    private Long foodId;

    @Column(name = "food_name", nullable = false, length = 100)
    private String foodName;

    @Column(name = "calories_kcal", nullable = false)
    private Double caloriesKcal;

    @Column(name = "carb_g", nullable = false)
    private Double carbG;

    @Column(name = "protein_g", nullable = false)
    private Double proteinG;

    @Column(name = "fat_g", nullable = false)
    private Double fatG;

    @Column(name = "base_amount_g", nullable = false)
    private Double baseAmountG;
}