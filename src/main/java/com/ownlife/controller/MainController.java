package com.ownlife.controller;

import com.ownlife.dto.DashboardSummaryDto;
import com.ownlife.dto.SessionMember;
import com.ownlife.service.DashboardService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final DashboardService dashboardService;

    @GetMapping({"/", "/main"})
    public String main(Model model, HttpSession session) {
        SessionMember loginMember = (SessionMember) session.getAttribute(AuthController.LOGIN_MEMBER);

        DashboardSummaryDto dashboard = new DashboardSummaryDto();

        if (loginMember != null) {
            dashboard = dashboardService.getDashboardSummary(loginMember.getMemberId());
        }

        model.addAttribute("pageTitle", "OWN LIFE");
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("centerFragment", "fragments/center-dashboard :: centerDashboard");

        return "main";
    }
}