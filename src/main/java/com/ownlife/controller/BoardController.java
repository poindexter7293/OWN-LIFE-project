package com.ownlife.controller;

import com.ownlife.service.BoardCommentService;
import com.ownlife.service.BoardPostService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardController {

    private final BoardPostService boardPostService;
    private final BoardCommentService boardCommentService;

    @GetMapping
    public String list(Model model, HttpSession session) {

        var loginMember = (com.ownlife.dto.SessionMember) session.getAttribute("loginMember");
        Long loginMemberId = (loginMember != null) ? loginMember.getMemberId() : null;

        model.addAttribute("pageTitle", "자유게시판");
        model.addAttribute("posts", boardPostService.findAllView());
        model.addAttribute("loginMemberId", loginMemberId);
        model.addAttribute("centerFragment", "fragments/center-board-list :: centerBoardList");

        return "main";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model, HttpSession session) {

        var loginMember = (com.ownlife.dto.SessionMember) session.getAttribute("loginMember");
        Long loginMemberId = (loginMember != null) ? loginMember.getMemberId() : null;

        boardPostService.increaseViewCount(id);

        model.addAttribute("pageTitle", "게시글 상세");
        model.addAttribute("post", boardPostService.findViewById(id));
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
    public String write(@RequestParam String title,
                        @RequestParam String content,
                        HttpSession session) {

        var loginMember = (com.ownlife.dto.SessionMember) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/login";
        }

        boardPostService.save(loginMember.getMemberId(), title, content);

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
                       @RequestParam String title,
                       @RequestParam String content) {
        boardPostService.update(id, title, content);
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
                               @RequestParam String content,
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
                                @RequestParam Long postId,
                                HttpSession session) {

        var loginMember = (com.ownlife.dto.SessionMember) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/login";
        }

        boardCommentService.delete(commentId, loginMember.getMemberId());

        return "redirect:/board/" + postId;
    }

}