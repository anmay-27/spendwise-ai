package com.anmay.spendwise.controller;

import com.anmay.spendwise.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
  @org.springframework.beans.factory.annotation.Autowired
  private com.anmay.spendwise.security.CurrentUserService currentUser;

  @org.springframework.beans.factory.annotation.Autowired
  private com.anmay.spendwise.service.JsonCache cache;

  private final AnalyticsService analyticsService;

  public DashboardController(AnalyticsService analyticsService) {
    this.analyticsService = analyticsService;
  }

  @GetMapping
  public Object dashboard(@RequestParam(required = false) Long ignoredUserId) {
    Long uid = currentUser.currentUserId();
    return cache.get(
        "dashboard",
        uid,
        java.time.YearMonth.now().toString(),
        () -> analyticsService.dashboard(uid));
  }
}
