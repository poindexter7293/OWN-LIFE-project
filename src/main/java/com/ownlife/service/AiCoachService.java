package com.ownlife.service;

import com.ownlife.dto.AiCoachResponseDto;

public interface AiCoachService {
    AiCoachResponseDto getRecommendation(Long memberId, String type);
}