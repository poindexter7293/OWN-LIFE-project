package com.ownlife.controller;

import com.ownlife.entity.BoardPost;
import com.ownlife.service.BoardPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardController {

    private final BoardPostService boardPostService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pageTitle", "자유게시판");
        model.addAttribute("posts", boardPostService.findAll());
        model.addAttribute("centerFragment", "fragments/center-board-list :: centerBoardList");
        return "main";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        boardPostService.increaseViewCount(id);
        model.addAttribute("pageTitle", "게시글 상세");
        model.addAttribute("post", boardPostService.findById(id));
        model.addAttribute("centerFragment", "fragments/center-board-detail :: centerBoardDetail");
        return "main";
    }

    @GetMapping("/write")
    public String writeForm() {
        return "board/write";
    }

    @PostMapping("/write")
    public String write(@RequestParam String title,
                        @RequestParam String content) {
        boardPostService.save(title, content);
        return "redirect:/board";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("post", boardPostService.findById(id));
        return "board/edit";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id,
                       @RequestParam String title,
                       @RequestParam String content) {
        boardPostService.update(id, title, content);
        return "redirect:/board/" + id;
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable("id") Long id) {
        boardPostService.delete(id);
        return "redirect:/board";
    }
}