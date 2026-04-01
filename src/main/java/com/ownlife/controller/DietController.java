package com.ownlife.controller;

import com.ownlife.dto.SessionMember;
import com.ownlife.service.MealService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/diet")
public class DietController {

    private final MealService mealService;

    @GetMapping
    public String dietPage(@RequestParam(value = "date", required = false) String date,
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
        model.addAttribute("foodList", mealService.getAllFoods());
        model.addAttribute("mealLogs", mealService.getMealLogsByDate(memberId, selectedDate));
        model.addAttribute("dailySummary", mealService.getDailySummary(memberId, selectedDate));
        model.addAttribute("extraCssFiles", List.of("/css/diet.css"));
        model.addAttribute("centerFragment", "fragments/center-diet-main :: centerDietMain");

        return "main";
    }

    @PostMapping("/add")
    public String addDiet(@RequestParam("mealDate") String mealDate,
                          @RequestParam("mealType") String mealType,
                          @RequestParam("foodId") Long foodId,
                          @RequestParam("count") int count,
                          HttpSession session) {

        SessionMember loginMember = getLoginMember(session);
        if (loginMember == null) {
            return "redirect:/login";
        }

        Long memberId = loginMember.getMemberId();

        mealService.addMeal(memberId, LocalDate.parse(mealDate), mealType, foodId, count);

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