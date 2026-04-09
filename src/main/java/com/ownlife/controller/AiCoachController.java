package com.ownlife.controller;

import com.ownlife.dto.AiCoachResponseDto;
import com.ownlife.dto.SessionMember;
import com.ownlife.service.AiCoachService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-coach")
@RequiredArgsConstructor
public class AiCoachController {

    private final AiCoachService aiCoachService;

    @GetMapping("/recommend")
    public AiCoachResponseDto recommend(@RequestParam("type") String type, HttpSession session) {
        SessionMember loginMember = (SessionMember) session.getAttribute("loginMember");

        if (loginMember == null) {
            return AiCoachResponseDto.builder()
                    .type(type)
                    .title("로그인이 필요합니다")
                    .summary("AI 코치는 로그인 후 사용할 수 있어요.")
                    .messages(java.util.List.of("로그인 후 식단과 운동 기록을 바탕으로 맞춤 추천을 받을 수 있습니다."))
                    .hasData(false)
                    .build();
        }

        return aiCoachService.getRecommendation(loginMember.getMemberId(), type);
    }
}