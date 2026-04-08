package com.ownlife.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_image")
@Getter
@Setter
public class BoardImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private BoardPost post;

    @Column(name = "image_path", nullable = false, length = 255)
    private String imagePath;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "stored_name", nullable = false, length = 255)
    private String storedName;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
