package com.anmay.spendwise.controller;

import com.anmay.spendwise.dto.Responses.DashboardResponse;
import com.anmay.spendwise.security.CurrentUserService;
import com.anmay.spendwise.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final AnalyticsService analyticsService;
    private final CurrentUserService currentUserService;

    public DashboardController(AnalyticsService analyticsService,
                               CurrentUserService currentUserService) {
        this.analyticsService = analyticsService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public DashboardResponse dashboard() {
        return analyticsService.dashboard(currentUserService.currentUserId());
    }
}
