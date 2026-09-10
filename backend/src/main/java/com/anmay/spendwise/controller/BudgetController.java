package com.anmay.spendwise.controller;

import com.anmay.spendwise.dto.Requests.SetBudgetRequest;
import com.anmay.spendwise.dto.Responses.BudgetProgress;
import com.anmay.spendwise.service.BudgetService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {
  @org.springframework.beans.factory.annotation.Autowired
  private com.anmay.spendwise.security.CurrentUserService currentUser;

  @org.springframework.beans.factory.annotation.Autowired
  private com.anmay.spendwise.service.AnalyticsService analytics;

  private final BudgetService budgetService;

  public BudgetController(BudgetService budgetService) {
    this.budgetService = budgetService;
  }

  @GetMapping
  public List<BudgetProgress> list(
      @RequestParam(required = false) Long ignoredUserId,
      @RequestParam(required = false) String month) {
    return analytics.getBudgetProgress(
        currentUser.currentUserId(),
        month == null ? java.time.YearMonth.now() : java.time.YearMonth.parse(month));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    budgetService.delete(currentUser.currentUserId(), id);
  }

  @PostMapping
  public List<BudgetProgress> set(@Valid @RequestBody SetBudgetRequest request) {
    return budgetService.set(
        new SetBudgetRequest(
            currentUser.currentUserId(),
            request.categoryId(),
            request.monthlyLimit(),
            request.warningPercent(),
            request.month()));
  }
}
