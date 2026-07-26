package com.anmay.spendwise.controller;

import com.anmay.spendwise.dto.Responses.MonthlyReportResponse;
import com.anmay.spendwise.security.CurrentUserService;
import com.anmay.spendwise.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final AnalyticsService analyticsService;
    private final CurrentUserService currentUserService;

    public ReportController(AnalyticsService analyticsService,
                            CurrentUserService currentUserService) {
        this.analyticsService = analyticsService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/monthly")
    public MonthlyReportResponse monthly(@RequestParam(required = false) Integer year,
                                         @RequestParam(required = false) Integer month) {
        return analyticsService.monthlyReport(currentUserService.currentUserId(), year, month);
    }
}
