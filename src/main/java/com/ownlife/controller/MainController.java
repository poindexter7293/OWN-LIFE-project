package com.ownlife.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class MainController {

    @GetMapping({"/", "/main"})
    public String main(Model model) {
        model.addAttribute("pageTitle", "OWN LIFE");

        model.addAttribute("weight", 74.3);
        model.addAttribute("weightDiff", 0.3);

        model.addAttribute("burnedCalories", 540);
        model.addAttribute("burnedDiff", 325);

        model.addAttribute("intakeCalories", 2244);
        model.addAttribute("targetCalories", 2200);

        model.addAttribute("carbPercent", 61);
        model.addAttribute("fatPercent", 24);
        model.addAttribute("proteinPercent", 15);

        model.addAttribute("weightLabels",
                List.of("4/18", "4/19", "4/20", "4/21", "4/22", "4/23", "4/24"));
        model.addAttribute("weightData",
                List.of(71.8, 71.9, 71.3, 71.1, 70.8, 71.2, 72.4));

        model.addAttribute("centerFragment", "fragments/center-dashboard :: centerDashboard");

        return "main";
    }
}