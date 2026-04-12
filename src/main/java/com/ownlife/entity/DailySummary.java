package com.ownlife.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "daily_summary",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_daily_summary_member_date",
                        columnNames = {"member_id", "summary_date"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class DailySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_summary_id")
    private Long dailySummaryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "total_eat_kcal", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalEatKcal = BigDecimal.ZERO;

    @Column(name = "total_burned_kcal", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalBurnedKcal = BigDecimal.ZERO;

    @Column(name = "total_carb_g", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalCarbG = BigDecimal.ZERO;

    @Column(name = "total_protein_g", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalProteinG = BigDecimal.ZERO;

    @Column(name = "total_fat_g", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalFatG = BigDecimal.ZERO;

    @Column(name = "goal_eat_kcal")
    private Integer goalEatKcal;

    @Column(name = "goal_burned_kcal")
    private Integer goalBurnedKcal;

    @Column(name = "goal_weight", precision = 5, scale = 2)
    private BigDecimal goalWeight;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}