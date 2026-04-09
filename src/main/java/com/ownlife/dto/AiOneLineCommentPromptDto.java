package com.ownlife.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiOneLineCommentPromptDto {

    private final Long memberId;
    private final Integer goalEatKcal;
    private final Integer goalBurnedKcal;
    private final Integer todayIntakeCalories;
    private final Integer todayBurnedCalories;
    private final Integer intakePercent;
    private final Integer burnedPercent;
    private final Integer proteinGram;
    private final Integer proteinPercent;
    private final Integer carbGram;
    private final Integer fatGram;
    private final Integer streakDays;
    private final String weightGoalMessage;
    private final String lifestyleTitle;
    private final String lifestyleDescription;
}

