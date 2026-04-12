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
    private String weightDiffTone = "muted";
    private String goalWeightText = "";

    private int streakDays = 0;
    private String streakMessage = "기록 없음";
    private String streakTone = "danger";

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

    private SummaryChartDto summaryChart = new SummaryChartDto();

    @Getter
    @Setter
    public static class SummaryChartDto {
        private RangeChartDto week = new RangeChartDto();
        private RangeChartDto month = new RangeChartDto();
        private RangeChartDto year = new RangeChartDto();
    }

    @Getter
    @Setter
    public static class RangeChartDto {
        private List<String> labels = new ArrayList<>();

        private List<Double> weightKg = new ArrayList<>();

        private List<Double> totalEatKcal = new ArrayList<>();
        private List<Double> totalBurnedKcal = new ArrayList<>();
        private List<Double> totalCarbG = new ArrayList<>();
        private List<Double> totalProteinG = new ArrayList<>();
        private List<Double> totalFatG = new ArrayList<>();

        private List<Double> goalEatKcal = new ArrayList<>();
        private List<Double> goalBurnedKcal = new ArrayList<>();
        private List<Double> goalWeight = new ArrayList<>();
    }
}