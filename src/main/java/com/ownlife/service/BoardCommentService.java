package com.ownlife.service;

import com.ownlife.dto.BoardCommentViewDto;
import com.ownlife.entity.BoardComment;
import com.ownlife.repository.BoardCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardCommentService {

    private final BoardCommentRepository boardCommentRepository;

    @Transactional(readOnly = true)
    public List<BoardComment> findByPostId(Long postId) {
        return boardCommentRepository.findByPostIdAndIsDeletedFalseAndParentCommentIdIsNullOrderByCommentIdAsc(postId);
    }

    @Transactional(readOnly = true)
    public List<BoardCommentViewDto> findViewByPostId(Long postId) {
        return boardCommentRepository.findViewByPostId(postId);
    }

    public void save(Long postId, Long memberId, String content) {
        BoardComment comment = new BoardComment();
        comment.setPostId(postId);
        comment.setMemberId(memberId);
        comment.setParentCommentId(null);
        comment.setContent(content);
        comment.setIsDeleted(false);

        boardCommentRepository.save(comment);
    }

    public void delete(Long commentId, Long loginMemberId) {
        BoardComment comment = boardCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글 없음"));

        if (!comment.getMemberId().equals(loginMemberId)) {
            throw new IllegalArgumentException("본인 댓글만 삭제 가능");
        }

        comment.setIsDeleted(true);
    }
}