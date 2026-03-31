package com.ownlife.repository;

import com.ownlife.dto.BoardCommentViewDto;
import com.ownlife.entity.BoardComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BoardCommentRepository extends JpaRepository<BoardComment, Long> {

    List<BoardComment> findByPostIdAndIsDeletedFalseAndParentCommentIdIsNullOrderByCommentIdAsc(Long postId);

    @Query("""
        select new com.ownlife.dto.BoardCommentViewDto(
            c.commentId,
            c.postId,
            c.memberId,
            m.nickname,
            c.content,
            c.createdAt
        )
        from BoardComment c
        join Member m on c.memberId = m.memberId
        where c.postId = :postId
          and c.isDeleted = false
          and c.parentCommentId is null
        order by c.commentId asc
    """)
    List<BoardCommentViewDto> findViewByPostId(Long postId);
}