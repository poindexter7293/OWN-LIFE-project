package com.ownlife.dto;

import com.ownlife.entity.BoardImage;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class BoardPostViewDto {
    private Long postId;
    private Long memberId;
    private String nickname;
    private String title;
    private String content;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private List<BoardImage> images;

    public BoardPostViewDto(Long postId,
                            Long memberId,
                            String nickname,
                            String title,
                            String content,
                            Integer viewCount,
                            LocalDateTime createdAt) {
        this.postId = postId;
        this.memberId = memberId;
        this.nickname = nickname;
        this.title = title;
        this.content = content;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
        this.images = new ArrayList<>();
    }

    public BoardPostViewDto(Long postId,
                            Long memberId,
                            String nickname,
                            String title,
                            String content,
                            Integer viewCount,
                            LocalDateTime createdAt,
                            List<BoardImage> images) {
        this.postId = postId;
        this.memberId = memberId;
        this.nickname = nickname;
        this.title = title;
        this.content = content;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
        this.images = images;
    }
}