package com.ownlife.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachSummaryDto {

    private boolean hasMealData;
    private boolean hasExerciseData;
    private boolean hasWeightData;

    private double avgCalories;
    private double avgProtein;
    private double avgCarbs;
    private double avgFat;

    private int exerciseDays;

    private Double currentWeight;
    private Double weightChange;

    private String overallStatus;
}