package com.ownlife.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AiCoachResponseDto {
    private String type;              // diet, exercise, analysis, feedback
    private String title;             // 화면에 보여줄 제목
    private String summary;           // 한줄 요약
    private List<String> messages;    // 상세 추천 문구
    private boolean hasData;          // 기록 존재 여부
}