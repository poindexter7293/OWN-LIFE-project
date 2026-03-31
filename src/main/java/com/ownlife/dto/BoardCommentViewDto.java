package com.ownlife.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BoardCommentViewDto {
    private Long commentId;
    private Long postId;
    private Long memberId;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;
}