package com.ownlife.service;

import com.ownlife.dto.BoardPostViewDto;
import com.ownlife.entity.BoardImage;
import com.ownlife.entity.BoardPost;
import com.ownlife.repository.BoardImageRepository;
import com.ownlife.repository.BoardPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardPostService {

    private final BoardPostRepository boardPostRepository;
    private final BoardImageRepository boardImageRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public List<BoardPost> findAll() {
        return boardPostRepository.findByIsDeletedFalseOrderByPostIdDesc();
    }

    @Transactional(readOnly = true)
    public List<BoardPostViewDto> findAllView() {
        return boardPostRepository.findAllWithNickname();
    }

    @Transactional(readOnly = true)
    public BoardPost findById(Long postId) {
        return boardPostRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다. id=" + postId));
    }

    @Transactional(readOnly = true)
    public BoardPostViewDto findViewById(Long postId) {
        return boardPostRepository.findViewByPostId(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다. id=" + postId));
    }

    public BoardPost save(Long memberId, String title, String content, MultipartFile[] images) {
        BoardPost boardPost = new BoardPost();
        boardPost.setMemberId(memberId);
        boardPost.setTitle(title);
        boardPost.setContent(content);
        boardPost.setViewCount(0);
        boardPost.setIsDeleted(false);

        BoardPost savedPost = boardPostRepository.save(boardPost);

        saveBoardImages(savedPost, images);

        return savedPost;
    }

    public void update(Long postId, String title, String content,
                       MultipartFile[] newImages, List<Long> deleteImageIds) {

        BoardPost boardPost = findById(postId);
        boardPost.setTitle(title);
        boardPost.setContent(content);

        // 1. 삭제할 이미지 처리
        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            List<BoardImage> existingImages = boardImageRepository.findByPostPostId(postId);

            for (BoardImage image : existingImages) {
                if (deleteImageIds.contains(image.getImageId())) {
                    deleteImageFile(image.getStoredName());
                }
            }

            boardImageRepository.deleteByImageIdIn(deleteImageIds);
        }

        // 2. 현재 남아있는 이미지 개수 확인
        int remainImageCount = boardImageRepository.findByPostPostId(postId).size();

        // 3. 새 이미지 개수 계산
        int newImageCount = 0;
        if (newImages != null) {
            for (MultipartFile image : newImages) {
                if (image != null && !image.isEmpty()) {
                    newImageCount++;
                }
            }
        }

        // 4. 최대 3장 제한
        if (remainImageCount + newImageCount > 3) {
            throw new IllegalArgumentException("이미지는 최대 3장까지 유지할 수 있습니다.");
        }

        // 5. 새 이미지 저장
        saveBoardImages(boardPost, newImages);
    }

    public void delete(Long postId, Long loginMemberId) {
        BoardPost post = findById(postId);

        if (!post.getMemberId().equals(loginMemberId)) {
            throw new IllegalArgumentException("본인 글만 삭제 가능");
        }

        post.setIsDeleted(true);
    }

    public void increaseViewCount(Long postId) {
        BoardPost boardPost = findById(postId);
        boardPost.setViewCount(boardPost.getViewCount() + 1);
    }

    private void saveBoardImages(BoardPost post, MultipartFile[] images) {
        if (images == null || images.length == 0) {
            return;
        }

        Path boardUploadPath = Paths.get(uploadDir, "board");

        try {
            Files.createDirectories(boardUploadPath);
        } catch (IOException e) {
            throw new RuntimeException("업로드 폴더 생성 실패", e);
        }

        int sortOrder = boardImageRepository.findByPostPostId(post.getPostId()).size();
        int uploadedCount = 0;

        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                continue;
            }

            if (uploadedCount >= 3) {
                break;
            }

            String contentType = image.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
            }

            String originalName = image.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) {
                originalName = "image";
            }
            String storedName = UUID.randomUUID() + "_" + originalName;
            Path savePath = boardUploadPath.resolve(storedName);

            System.out.println("uploadDir = " + uploadDir);
            System.out.println("savePath = " + savePath.toAbsolutePath());

            try {
                image.transferTo(savePath.toFile());
                System.out.println("exists after save = " + Files.exists(savePath));
            } catch (IOException e) {
                throw new RuntimeException("이미지 저장 실패", e);
            }

            BoardImage boardImage = new BoardImage();
            boardImage.setPost(post);
            boardImage.setOriginalName(originalName);
            boardImage.setStoredName(storedName);
            boardImage.setImagePath("/uploads/board/" + storedName);
            boardImage.setSortOrder(sortOrder++);

            boardImageRepository.save(boardImage);
            uploadedCount++;
        }
    }

    private void deleteImageFile(String storedName) {
        try {
            Path filePath = Paths.get(uploadDir, "board", storedName);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("이미지 파일 삭제 실패", e);
        }
    }
}