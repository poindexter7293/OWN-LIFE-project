package com.ownlife.controller;

import com.ownlife.dto.DietPageDataDto;
import com.ownlife.dto.SessionMember;
import com.ownlife.service.MealService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
                           HttpSession session,
                           Model model) {

        SessionMember loginMember = (SessionMember) session.getAttribute("loginMember");
        if (loginMember == null) {
            return "redirect:/login";
        }

        Long memberId = loginMember.getMemberId();
        LocalDate selectedDate = (date != null && !date.isBlank()) ? LocalDate.parse(date) : LocalDate.now();

        DietPageDataDto dietPageData = mealService.getDietPageData(memberId, selectedDate);

        model.addAttribute("pageTitle", "식단 관리 | OWN LIFE");
        model.addAttribute("dietPageData", dietPageData);
        model.addAttribute("foodList", mealService.getAllFoods());
        model.addAttribute("centerFragment", "fragments/center-diet-main :: centerDietMain");

        model.addAttribute("extraCssFiles", List.of("/css/diet.css"));
        model.addAttribute("extraJsFiles", List.of("/js/diet.js"));
        model.addAttribute("period", "day");

        return "main";
    }

    @PostMapping("/add")
    public String addDiet(@RequestParam("mealDate") String mealDate,
                          @RequestParam("mealType") String mealType,
                          @RequestParam("inputMode") String inputMode,
                          @RequestParam(value = "foodId", required = false) Long foodId,
                          @RequestParam(value = "count", defaultValue = "1.0") double count,
                          @RequestParam(value = "saveAsFood", required = false) String saveAsFood,
                          @RequestParam(value = "customFoodName", required = false) String customFoodName,
                          @RequestParam(value = "customBaseAmountG", required = false) Double customBaseAmountG,
                          @RequestParam(value = "customCaloriesKcal", required = false) Double customCaloriesKcal,
                          @RequestParam(value = "customCarbG", required = false) Double customCarbG,
                          @RequestParam(value = "customProteinG", required = false) Double customProteinG,
                          @RequestParam(value = "customFatG", required = false) Double customFatG,
                          @RequestParam(value = "selectedFoodsJson", required = false) String selectedFoodsJson,
                          HttpSession session) {

        SessionMember loginMember = getLoginMember(session);
        if (loginMember == null) {
            return "redirect:/login";
        }

        Long memberId = loginMember.getMemberId();
        LocalDate dateValue = LocalDate.parse(mealDate);

        if ("custom".equals(inputMode)) {
            mealService.addCustomMeal(
                    memberId,
                    dateValue,
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
            mealService.addMeals(memberId, dateValue, mealType, selectedFoodsJson);
        }

        return "redirect:/diet?date=" + mealDate;
    }

    @PostMapping("/delete-group")
    @ResponseBody
    public ResponseEntity<DietPageDataDto> deleteMealGroup(@RequestParam("mealType") String mealType,
                                                           @RequestParam("date") String date,
                                                           HttpSession session) {

        SessionMember loginMember = getLoginMember(session);
        if (loginMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long memberId = loginMember.getMemberId();
        LocalDate mealDate = LocalDate.parse(date);

        mealService.deleteMealGroup(memberId, mealDate, mealType);

        DietPageDataDto updatedData = mealService.getDietPageData(memberId, mealDate);

        return ResponseEntity.ok(updatedData);
    }

    private SessionMember getLoginMember(HttpSession session) {
        return (SessionMember) session.getAttribute(AuthController.LOGIN_MEMBER);
    }
}