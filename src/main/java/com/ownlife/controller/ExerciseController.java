package com.ownlife.controller;

import com.ownlife.dto.SessionMember;
import com.ownlife.service.ExerciseService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/exercise")
public class ExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping
    public String exercisePage(@RequestParam(value = "date", required = false) LocalDate date,
                               Model model,
                               HttpSession session) {
        SessionMember loginMember = (SessionMember) session.getAttribute(AuthController.LOGIN_MEMBER);
        if (loginMember == null) {
            return "redirect:/login";
        }

        ExerciseService.ExercisePageData pageData = exerciseService.getPageData(loginMember.getMemberId(), date);

        model.addAttribute("pageTitle", "운동 일지");
        model.addAttribute("centerFragment", "fragments/center-exercise :: centerExercise");
        model.addAttribute("extraCssFiles", List.of("/css/exercise.css"));
        model.addAttribute("extraJsFiles", List.of("/js/exercise.js"));
        model.addAttribute("exerciseData", pageData);
        model.addAttribute("countExerciseOptions", exerciseService.getCountOptions());
        model.addAttribute("timeExerciseOptions", exerciseService.getTimeOptions());
        model.addAttribute("routeExerciseOptions", exerciseService.getRouteOptions());
        return "main";
    }

    @GetMapping("/chart")
    @ResponseBody
    public ExerciseService.ExerciseChartData chartData(
            @RequestParam(value = "date", required = false) LocalDate date,
            @RequestParam(value = "period", defaultValue = "WEEK") ExerciseService.ChartPeriod period,
            HttpSession session
    ) {
        SessionMember loginMember = (SessionMember) session.getAttribute(AuthController.LOGIN_MEMBER);
        if (loginMember == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        return exerciseService.getChartData(loginMember.getMemberId(), date, period);
    }

    @PostMapping("/direct")
    public String addDirect(@RequestParam("exerciseDate") LocalDate exerciseDate,
                            @RequestParam("exerciseName") String exerciseName,
                            @RequestParam("burnedKcal") Integer burnedKcal,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        return handleAction(session, redirectAttributes, exerciseDate, () ->
                exerciseService.addDirectExercise(currentMemberId(session), exerciseDate, exerciseName, burnedKcal)
        );
    }

    @PostMapping("/quick/count")
    public String addQuickCount(@RequestParam("exerciseDate") LocalDate exerciseDate,
                                @RequestParam("exerciseTypeId") Long exerciseTypeId,
                                @RequestParam("setsCount") Integer setsCount,
                                @RequestParam("repsCount") Integer repsCount,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        return handleAction(session, redirectAttributes, exerciseDate, () ->
                exerciseService.addQuickCountExercise(currentMemberId(session), exerciseDate, exerciseTypeId, setsCount, repsCount)
        );
    }

    @PostMapping("/quick/time")
    public String addQuickTime(@RequestParam("exerciseDate") LocalDate exerciseDate,
                               @RequestParam("exerciseTypeId") Long exerciseTypeId,
                               @RequestParam("durationMin") Integer durationMin,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        return handleAction(session, redirectAttributes, exerciseDate, () ->
                exerciseService.addQuickTimeExercise(currentMemberId(session), exerciseDate, exerciseTypeId, durationMin)
        );
    }

    @PostMapping("/{exerciseLogId}/delete")
    public String delete(@PathVariable("exerciseLogId") Long exerciseLogId,
                         @RequestParam("exerciseDate") LocalDate exerciseDate,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        return handleAction(session, redirectAttributes, exerciseDate, () ->
                exerciseService.deleteExercise(currentMemberId(session), exerciseLogId)
        );
    }

    private String handleAction(HttpSession session,
                                RedirectAttributes redirectAttributes,
                                LocalDate exerciseDate,
                                Runnable action) {
        SessionMember loginMember = (SessionMember) session.getAttribute(AuthController.LOGIN_MEMBER);
        if (loginMember == null) {
            return "redirect:/login";
        }

        try {
            action.run();
            redirectAttributes.addFlashAttribute("exerciseSuccessMessage", "운동 기록이 저장되었습니다.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("exerciseErrorMessage", exception.getMessage());
        }

        return "redirect:/exercise?date=" + exerciseDate;
    }

    private Long currentMemberId(HttpSession session) {
        SessionMember loginMember = (SessionMember) session.getAttribute(AuthController.LOGIN_MEMBER);
        if (loginMember == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        return loginMember.getMemberId();
    }
}