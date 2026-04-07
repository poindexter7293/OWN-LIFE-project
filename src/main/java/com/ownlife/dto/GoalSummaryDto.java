package com.ownlife.dto;

import lombok.Data;

@Data
public class GoalSummaryDto {
    private double goalKcal;
    private double totalKcal;
    private double remainingKcal;
    private double exceededKcal;
    private double goalPercent;
}