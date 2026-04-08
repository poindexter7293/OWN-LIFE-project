package com.ownlife.repository;

import com.ownlife.entity.BoardImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardImageRepository extends JpaRepository<BoardImage, Long> {
    List<BoardImage> findByPostPostIdOrderBySortOrderAscImageIdAsc(Long postId);

    List<BoardImage> findByPostPostId(Long postId);

    void deleteByImageIdIn(List<Long> imageIds);
}