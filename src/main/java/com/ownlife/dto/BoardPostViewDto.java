package com.ownlife.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BoardPostViewDto {
    private Long postId;
    private Long memberId;
    private String nickname;
    private String title;
    private String content;
    private Integer viewCount;
    private LocalDateTime createdAt;
}