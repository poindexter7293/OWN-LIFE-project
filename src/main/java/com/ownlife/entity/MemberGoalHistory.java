package com.ownlife.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_goal_history")
@Getter
@Setter
@NoArgsConstructor
public class MemberGoalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "goal_history_id")
    private Long goalHistoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "goal_weight", precision = 5, scale = 2)
    private BigDecimal goalWeight;

    @Column(name = "goal_eat_kcal")
    private Integer goalEatKcal;

    @Column(name = "goal_burned_kcal")
    private Integer goalBurnedKcal;

    @Column(name = "changed_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime changedAt;
}

