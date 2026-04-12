package com.ownlife.controller;

import com.ownlife.service.DailySummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/daily-summary")
public class AdminDailySummaryController {

    private final DailySummaryService dailySummaryService;

    @GetMapping("/run")
    public String run(@RequestParam String date) {
        dailySummaryService.summarizeAllMembers(LocalDate.parse(date));
        return "OK";
    }
}