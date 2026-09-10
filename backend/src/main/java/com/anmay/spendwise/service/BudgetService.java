package com.anmay.spendwise.service;

import com.anmay.spendwise.dto.Requests.SetBudgetRequest;
import com.anmay.spendwise.dto.Responses.BudgetProgress;
import com.anmay.spendwise.entity.*;
import com.anmay.spendwise.repository.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {
  @org.springframework.beans.factory.annotation.Autowired
  private com.anmay.spendwise.events.EventOutbox events;

  private final AppUserRepository userRepository;
  private final CategoryRepository categoryRepository;
  private final BudgetRepository budgetRepository;
  private final AnalyticsService analyticsService;

  public BudgetService(
      AppUserRepository userRepository,
      CategoryRepository categoryRepository,
      BudgetRepository budgetRepository,
      AnalyticsService analyticsService) {
    this.userRepository = userRepository;
    this.categoryRepository = categoryRepository;
    this.budgetRepository = budgetRepository;
    this.analyticsService = analyticsService;
  }

  @Transactional(readOnly = true)
  public List<BudgetProgress> list(Long userId) {
    return analyticsService.getBudgetProgress(userId);
  }

  @Transactional
  public void delete(Long uid, Long id) {
    var budget =
        budgetRepository
            .findById(id)
            .filter(b -> b.getUser().getId().equals(uid))
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Budget not found"));
    budgetRepository.delete(budget);
    events.append("BUDGET_UPDATED", null, uid, java.util.Map.of("month", budget.getMonth()));
  }

  @Transactional
  public List<BudgetProgress> set(SetBudgetRequest request) {
    AppUser user =
        userRepository
            .findById(request.userId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    Category category =
        categoryRepository
            .findById(request.categoryId())
            .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    if (!category.getUser().getId().equals(user.getId())) {
      throw new IllegalArgumentException("Category does not belong to this user");
    }
    Budget budget =
        budgetRepository
            .findByUserIdAndCategoryIdAndMonth(
                user.getId(),
                category.getId(),
                (request.month() == null
                        ? java.time.YearMonth.now()
                        : java.time.YearMonth.parse(request.month()))
                    .toString())
            .orElseGet(
                () -> new Budget(user, category, request.monthlyLimit(), request.warningPercent()));
    budget.setMonth(
        (request.month() == null
                ? java.time.YearMonth.now()
                : java.time.YearMonth.parse(request.month()))
            .toString());
    budget.setMonthlyLimit(request.monthlyLimit());
    budget.setWarningPercent(request.warningPercent());
    budgetRepository.saveAndFlush(budget);
    events.append(
        "BUDGET_UPDATED", null, user.getId(), java.util.Map.of("month", budget.getMonth()));
    return analyticsService.getBudgetProgress(
        user.getId(), java.time.YearMonth.parse(budget.getMonth()));
  }
}
