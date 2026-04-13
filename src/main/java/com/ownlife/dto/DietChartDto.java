package com.ownlife.dto;

import lombok.Data;
import java.util.List;

@Data
public class DietChartDto {
    private List<String> labels;
    private List<Double> calories;
    private List<Double> goalKcalSeries;
    //
    private double goalKcal;
}