package com.anmay.spendwise.controller;

import com.anmay.spendwise.dto.Requests.SetBudgetRequest;
import com.anmay.spendwise.dto.Responses.BudgetProgress;
import com.anmay.spendwise.security.CurrentUserService;
import com.anmay.spendwise.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {
    private final BudgetService budgetService;
    private final CurrentUserService currentUserService;

    public BudgetController(BudgetService budgetService,
                            CurrentUserService currentUserService) {
        this.budgetService = budgetService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<BudgetProgress> list() {
        return budgetService.list(currentUserService.currentUserId());
    }

    @PostMapping
    public List<BudgetProgress> set(@Valid @RequestBody SetBudgetRequest request) {
        return budgetService.set(currentUserService.currentUserId(), request);
    }
}
