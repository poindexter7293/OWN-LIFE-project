package com.ownlife.repository;

import com.ownlife.entity.BoardPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardPostRepository extends JpaRepository<BoardPost, Long> {

    List<BoardPost> findByIsDeletedFalseOrderByPostIdDesc();

    Optional<BoardPost> findByPostIdAndIsDeletedFalse(Long postId);
}