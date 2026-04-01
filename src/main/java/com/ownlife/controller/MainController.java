package com.ownlife.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class MainController {

    @GetMapping({"/", "/main"})
    public String main(Model model) {
        double weight = 74.3;
        double weightDiff = 0.3;
        int streakDays = 7;

        int burnedCalories = 540;
        int targetCalories = 2200;
        int intakeCalories = 1340;

        int carbGram = 201;
        int fatGram = 52;
        int proteinGram = 118;

        int carbPercent = 46;
        int fatPercent = 24;
        int proteinPercent = 30;

        List<String> weightLabels = List.of("4/18", "4/19", "4/20", "4/21", "4/22", "4/23", "4/24");
        List<Double> weightData = List.of(71.8, 71.9, 71.3, 71.1, 70.8, 71.2, 72.4);

        int intakePercent = targetCalories > 0
                ? Math.min(100, (int) Math.round((intakeCalories * 100.0) / targetCalories))
                : 0;

        int remainingBurnCalories = Math.max(0, targetCalories - burnedCalories);

        model.addAttribute("pageTitle", "OWN LIFE");
        model.addAttribute("weight", weight);
        model.addAttribute("weightDiff", weightDiff);
        model.addAttribute("weightDiffText", String.format("전일 대비 %+.1fkg", weightDiff));
        model.addAttribute("streakDays", streakDays);
        model.addAttribute("streakMessage", "연속 기록 중");
        model.addAttribute("burnedCalories", burnedCalories);
        model.addAttribute("remainingBurnCalories", remainingBurnCalories);
        model.addAttribute("intakeCalories", intakeCalories);
        model.addAttribute("targetCalories", targetCalories);
        model.addAttribute("intakePercent", intakePercent);
        model.addAttribute("carbGram", carbGram);
        model.addAttribute("fatGram", fatGram);
        model.addAttribute("proteinGram", proteinGram);
        model.addAttribute("carbPercent", carbPercent);
        model.addAttribute("fatPercent", fatPercent);
        model.addAttribute("proteinPercent", proteinPercent);
        model.addAttribute("weightLabels", weightLabels);
        model.addAttribute("weightData", weightData);
        model.addAttribute("centerFragment", "fragments/center-dashboard :: centerDashboard");

        return "main";
    }
}