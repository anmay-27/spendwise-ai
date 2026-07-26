package com.anmay.spendwise.controller;

import com.anmay.spendwise.dto.Responses.DashboardResponse;
import com.anmay.spendwise.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final AnalyticsService analyticsService;
    public DashboardController(AnalyticsService analyticsService) { this.analyticsService = analyticsService; }

    @GetMapping
    public DashboardResponse dashboard(@RequestParam(defaultValue = "1") Long userId) {
        return analyticsService.dashboard(userId);
    }
}
