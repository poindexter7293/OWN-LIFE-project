package com.ownlife.controller;

import com.ownlife.dto.BoardPostViewDto;
import com.ownlife.entity.BoardPost;
import com.ownlife.service.BoardCommentService;
import com.ownlife.service.BoardPostService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Controller
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardController {

    private final BoardPostService boardPostService;
    private final BoardCommentService boardCommentService;

    @GetMapping
    public String list(@RequestParam(value = "page", defaultValue = "0") int page,
                       Model model,
                       HttpSession session) {

        var loginMember = (com.ownlife.dto.SessionMember) session.getAttribute("loginMember");
        Long loginMemberId = (loginMember != null) ? loginMember.getMemberId() : null;

        Page<BoardPostViewDto> postPage = boardPostService.findAllView(page, 10);

        model.addAttribute("pageTitle", "자유게시판");
        model.addAttribute("posts", postPage.getContent());
        model.addAttribute("postPage", postPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("loginMemberId", loginMemberId);
        model.addAttribute("centerFragment", "fragments/center-board-list :: centerBoardList");

        return "main";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model, HttpSession session) {

        var loginMember = (com.ownlife.dto.SessionMember) session.getAttribute("loginMember");
        Long loginMemberId = (loginMember != null) ? loginMember.getMemberId() : null;

        boardPostService.increaseViewCount(id);

        BoardPostViewDto post = boardPostService.findViewById(id);
        BoardPost boardPost = boardPostService.findById(id);
        post.setImages(boardPost.getImages());

        model.addAttribute("pageTitle", "게시글 상세");
        model.addAttribute("post", post);
        model.addAttribute("comments", boardCommentService.findViewByPostId(id));
        model.addAttribute("loginMemberId", loginMemberId);
        model.addAttribute("centerFragment", "fragments/center-board-detail :: centerBoardDetail");

        return "main";
    }

    @GetMapping("/write")
    public String writeForm(Model model) {
        model.addAttribute("pageTitle", "글쓰기");
        model.addAttribute("centerFragment", "fragments/center-board-write :: centerBoardWrite");
        return "main";
    }

    @PostMapping("/write")
    public String write(@RequestParam("title") String title,
                        @RequestParam("content") String content,
                        @RequestParam(value = "images", required = false) MultipartFile[] images,
                        HttpSession session) {

        var loginMember = (com.ownlife.dto.SessionMember) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/login";
        }

        boardPostService.save(loginMember.getMemberId(), title, content,images);

        return "redirect:/board";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("pageTitle", "글수정");
        model.addAttribute("post", boardPostService.findById(id));
        model.addAttribute("centerFragment", "fragments/center-board-edit :: centerBoardEdit");
        return "main";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id,
                       @RequestParam("title") String title,
                       @RequestParam("content") String content,
                       @RequestParam(value = "newImages", required = false) MultipartFile[] newImages,
                       @RequestParam(value = "deleteImageIds", required = false) List<Long> deleteImageIds) {

        boardPostService.update(id, title, content, newImages, deleteImageIds);
        return "redirect:/board/" + id;
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable("id") Long id,
                         HttpSession session) {

        var loginMember = (com.ownlife.dto.SessionMember) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/login";
        }

        boardPostService.delete(id, loginMember.getMemberId());
        return "redirect:/board";
    }

    @PostMapping("/{id}/comment")
    public String writeComment(@PathVariable("id") Long id,
                               @RequestParam("content") String content,
                               HttpSession session) {

        var loginMember = (com.ownlife.dto.SessionMember) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/login";
        }

        boardCommentService.save(id, loginMember.getMemberId(), content);

        return "redirect:/board/" + id;
    }

    @PostMapping("/comment/delete/{commentId}")
    public String deleteComment(@PathVariable("commentId") Long commentId,
                                @RequestParam("postId") Long postId,
                                HttpSession session) {

        var loginMember = (com.ownlife.dto.SessionMember) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/login";
        }

        boardCommentService.delete(commentId, loginMember.getMemberId());

        return "redirect:/board/" + postId;
    }

}