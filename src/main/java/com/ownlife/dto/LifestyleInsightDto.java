package com.ownlife.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LifestyleInsightDto {

    private final String title;
    private final String description;
    private final String tone;
}

