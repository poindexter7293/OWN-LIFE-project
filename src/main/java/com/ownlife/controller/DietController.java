package com.ownlife.controller;

import com.ownlife.dto.SessionMember;
import com.ownlife.service.MealService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/diet")
public class DietController {

    private final MealService mealService;

    @GetMapping
    public String dietPage(@RequestParam(value = "date", required = false) String date,
                           @RequestParam(value = "period", defaultValue = "day") String period,
                           Model model,
                           HttpSession session) {

        SessionMember loginMember = getLoginMember(session);
        if (loginMember == null) {
            return "redirect:/login";
        }

        LocalDate selectedDate = (date == null || date.isBlank())
                ? LocalDate.now()
                : LocalDate.parse(date);

        Long memberId = loginMember.getMemberId();

        model.addAttribute("pageTitle", "식단관리");
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("period", period);

        model.addAttribute("foodList", mealService.getAllFoods());
        model.addAttribute("mealLogs", mealService.getMealLogsByDate(memberId, selectedDate));
        model.addAttribute("dailySummary", mealService.getDailySummary(memberId, selectedDate));
        model.addAttribute("dietGoalSummary", mealService.getDietGoalSummary(memberId, selectedDate));

        // 일단 day만 연결
        model.addAttribute("weeklyIntakeSummary", mealService.getWeeklyIntakeSummary(memberId, selectedDate));

        model.addAttribute("extraCssFiles", List.of("/css/diet.css"));
        model.addAttribute("extraJsFiles", List.of("/js/diet.js"));
        model.addAttribute("centerFragment", "fragments/center-diet-main :: centerDietMain");

        return "main";
    }

    @PostMapping("/add")
    public String addDiet(@RequestParam("mealDate") String mealDate,
                          @RequestParam("mealType") String mealType,
                          @RequestParam("inputMode") String inputMode,
                          @RequestParam(value = "foodId", required = false) Long foodId,
                          @RequestParam(value = "count", defaultValue = "1") int count,
                          @RequestParam(value = "saveAsFood", required = false) String saveAsFood,
                          @RequestParam(value = "customFoodName", required = false) String customFoodName,
                          @RequestParam(value = "customBaseAmountG", required = false) Double customBaseAmountG,
                          @RequestParam(value = "customCaloriesKcal", required = false) Double customCaloriesKcal,
                          @RequestParam(value = "customCarbG", required = false) Double customCarbG,
                          @RequestParam(value = "customProteinG", required = false) Double customProteinG,
                          @RequestParam(value = "customFatG", required = false) Double customFatG,
                          HttpSession session) {

        SessionMember loginMember = getLoginMember(session);
        if (loginMember == null) {
            return "redirect:/login";
        }

        Long memberId = loginMember.getMemberId();
        LocalDate date = LocalDate.parse(mealDate);

        if ("custom".equals(inputMode)) {
            mealService.addCustomMeal(
                    memberId,
                    date,
                    mealType,
                    customFoodName,
                    customBaseAmountG,
                    count,
                    customCaloriesKcal,
                    customCarbG,
                    customProteinG,
                    customFatG,
                    saveAsFood != null
            );
        } else {
            mealService.addMeal(memberId, date, mealType, foodId, count);
        }

        return "redirect:/diet?date=" + mealDate;
    }

    @PostMapping("/delete/{mealLogId}")
    public String deleteDiet(@PathVariable("mealLogId") Long mealLogId,
                             @RequestParam("date") String date,
                             HttpSession session) {

        SessionMember loginMember = getLoginMember(session);
        if (loginMember == null) {
            return "redirect:/login";
        }

        Long memberId = loginMember.getMemberId();

        mealService.deleteMeal(memberId, mealLogId);

        return "redirect:/diet?date=" + date;
    }

    private SessionMember getLoginMember(HttpSession session) {
        return (SessionMember) session.getAttribute(AuthController.LOGIN_MEMBER);
    }

}