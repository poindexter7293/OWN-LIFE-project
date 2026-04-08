package com.ownlife.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LifestylePatternAnalysisDto {

    private final String title;
    private final String description;
    private final String periodLabel;
    private final List<LifestyleInsightDto> insights;

    public boolean hasInsights() {
        return insights != null && !insights.isEmpty();
    }
}

