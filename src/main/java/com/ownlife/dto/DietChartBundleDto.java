package com.ownlife.dto;

import lombok.Data;

@Data
public class DietChartBundleDto {
    private DietChartDto day;
    private DietChartDto week;
    private DietChartDto month;
    private DietChartDto year;
}
