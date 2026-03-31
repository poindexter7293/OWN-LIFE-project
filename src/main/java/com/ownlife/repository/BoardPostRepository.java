package com.ownlife.repository;

import com.ownlife.dto.BoardPostViewDto;
import com.ownlife.entity.BoardPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BoardPostRepository extends JpaRepository<BoardPost, Long> {

    List<BoardPost> findByIsDeletedFalseOrderByPostIdDesc();

    Optional<BoardPost> findByPostIdAndIsDeletedFalse(Long postId);

    @Query("""
        select new com.ownlife.dto.BoardPostViewDto(
            p.postId,
            p.memberId,
            m.nickname,
            p.title,
            p.content,
            p.viewCount,
            p.createdAt
        )
        from BoardPost p
        join Member m on p.memberId = m.memberId
        where p.isDeleted = false
        order by p.postId desc
    """)
    List<BoardPostViewDto> findAllWithNickname();

    @Query("""
        select new com.ownlife.dto.BoardPostViewDto(
            p.postId,
            p.memberId,
            m.nickname,
            p.title,
            p.content,
            p.viewCount,
            p.createdAt
        )
        from BoardPost p
        join Member m on p.memberId = m.memberId
        where p.postId = :postId
          and p.isDeleted = false
    """)
    Optional<BoardPostViewDto> findViewByPostId(Long postId);
}