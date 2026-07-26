package com.anmay.spendwise.controller;

import com.anmay.spendwise.dto.Responses.MonthlyReportResponse;
import com.anmay.spendwise.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final AnalyticsService analyticsService;
    public ReportController(AnalyticsService analyticsService) { this.analyticsService = analyticsService; }

    @GetMapping("/monthly")
    public MonthlyReportResponse monthly(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return analyticsService.monthlyReport(userId, year, month);
    }
}
