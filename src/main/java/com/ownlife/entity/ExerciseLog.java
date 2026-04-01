package com.ownlife.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "exercise_log")
@Getter
@Setter
@NoArgsConstructor
public class ExerciseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exercise_log_id")
    private Long exerciseLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_type_id", nullable = false)
    private ExerciseType exerciseType;

    @Column(name = "exercise_date", nullable = false)
    private LocalDate exerciseDate;

    @Column(name = "sets_count")
    private Integer setsCount;

    @Column(name = "reps_count")
    private Integer repsCount;

    @Column(name = "duration_min")
    private Integer durationMin;

    @Column(name = "distance_km", precision = 8, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "burned_kcal", nullable = false, precision = 8, scale = 2)
    private BigDecimal burnedKcal;

    @Column(name = "memo", length = 255)
    private String memo;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
