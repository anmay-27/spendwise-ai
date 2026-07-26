package com.anmay.spendwise.service;

import com.anmay.spendwise.dto.Requests.SetBudgetRequest;
import com.anmay.spendwise.dto.Responses.BudgetProgress;
import com.anmay.spendwise.entity.AppUser;
import com.anmay.spendwise.entity.Budget;
import com.anmay.spendwise.entity.Category;
import com.anmay.spendwise.repository.AppUserRepository;
import com.anmay.spendwise.repository.BudgetRepository;
import com.anmay.spendwise.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BudgetService {
    private final AppUserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final AnalyticsService analyticsService;

    public BudgetService(AppUserRepository userRepository,
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
    public List<BudgetProgress> set(Long userId, SetBudgetRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (!category.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Category does not belong to this user");
        }

        Budget budget = budgetRepository.findByUserIdAndCategoryId(userId, category.getId())
                .orElseGet(() -> new Budget(
                        user,
                        category,
                        request.monthlyLimit(),
                        request.warningPercent()
                ));
        budget.setMonthlyLimit(request.monthlyLimit());
        budget.setWarningPercent(request.warningPercent());
        budgetRepository.save(budget);
        return analyticsService.getBudgetProgress(userId);
    }
}
