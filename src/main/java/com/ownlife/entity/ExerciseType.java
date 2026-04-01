package com.ownlife.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "exercise_type")
@Getter
@Setter
@NoArgsConstructor
public class ExerciseType {

    @Id
    @Column(name = "exercise_type_id")
    private Long exerciseTypeId;

    @Column(name = "exercise_name", nullable = false, length = 50)
    private String exerciseName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Category category;

    @Column(name = "kcal_per_rep", precision = 8, scale = 4)
    private BigDecimal kcalPerRep;

    @Column(name = "kcal_per_min", precision = 8, scale = 4)
    private BigDecimal kcalPerMin;

    @Column(name = "kcal_per_km", precision = 8, scale = 4)
    private BigDecimal kcalPerKm;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public enum Category {
        COUNT_SET,
        TIME,
        ROUTE,
        SELF
    }
}
