package com.anmay.spendwise.controller;

import com.anmay.spendwise.dto.Responses.MonthlyReportResponse;
import com.anmay.spendwise.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
  @org.springframework.beans.factory.annotation.Autowired
  private com.anmay.spendwise.security.CurrentUserService currentUser;

  private final AnalyticsService analyticsService;

  public ReportController(AnalyticsService analyticsService) {
    this.analyticsService = analyticsService;
  }

  @GetMapping("/monthly")
  public MonthlyReportResponse monthly(
      @RequestParam(required = false) Long ignoredUserId,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) Integer month) {
    return analyticsService.monthlyReport(currentUser.currentUserId(), year, month);
  }
}
