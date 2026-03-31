package com.ownlife.service;

import com.ownlife.entity.BoardPost;
import com.ownlife.repository.BoardPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardPostService {

    private final BoardPostRepository boardPostRepository;

    @Transactional(readOnly = true)
    public List<BoardPost> findAll() {
        return boardPostRepository.findByIsDeletedFalseOrderByPostIdDesc();
    }

    @Transactional(readOnly = true)
    public BoardPost findById(Long postId) {
        return boardPostRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다. id=" + postId));
    }

    public BoardPost save(String title, String content) {
        BoardPost boardPost = new BoardPost();
        boardPost.setMemberId(1L); // 로그인 연동 전까지 임시 고정
        boardPost.setTitle(title);
        boardPost.setContent(content);
        boardPost.setViewCount(0);
        boardPost.setIsDeleted(false);
        return boardPostRepository.save(boardPost);
    }

    public void update(Long postId, String title, String content) {
        BoardPost boardPost = findById(postId);
        boardPost.setTitle(title);
        boardPost.setContent(content);
    }

    public void delete(Long postId) {
        BoardPost boardPost = findById(postId);
        boardPost.setIsDeleted(true);
    }

    public void increaseViewCount(Long postId) {
        BoardPost boardPost = findById(postId);
        boardPost.setViewCount(boardPost.getViewCount() + 1);
    }
}