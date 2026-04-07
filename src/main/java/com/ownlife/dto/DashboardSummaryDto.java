package com.ownlife.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DashboardSummaryDto {

    private Double weight = 0.0;
    private Double weightDiff = 0.0;
    private String weightDiffText = "기록 없음";

    private int streakDays = 0;
    private String streakMessage = "기록 없음";

    private int burnedCalories = 0;
    private int burnedTargetCalories = 0;
    private int burnedPercent = 0;

    private int intakeCalories = 0;
    private int intakeTargetCalories = 0;
    private int intakePercent = 0;

    private int carbGram = 0;
    private int fatGram = 0;
    private int proteinGram = 0;

    private int carbPercent = 0;
    private int fatPercent = 0;
    private int proteinPercent = 0;

    private List<String> weightLabels = new ArrayList<>();
    private List<Double> weightData = new ArrayList<>();
}