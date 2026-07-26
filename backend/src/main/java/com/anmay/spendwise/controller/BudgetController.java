package com.anmay.spendwise.controller;

import com.anmay.spendwise.dto.Requests.SetBudgetRequest;
import com.anmay.spendwise.dto.Responses.BudgetProgress;
import com.anmay.spendwise.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {
    private final BudgetService budgetService;
    public BudgetController(BudgetService budgetService) { this.budgetService = budgetService; }

    @GetMapping
    public List<BudgetProgress> list(@RequestParam(defaultValue = "1") Long userId) {
        return budgetService.list(userId);
    }

    @PostMapping
    public List<BudgetProgress> set(@Valid @RequestBody SetBudgetRequest request) {
        return budgetService.set(request);
    }
}
