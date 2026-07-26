package com.anmay.spendwise.controller;

import com.anmay.spendwise.dto.Requests.UpdateCategoryRequest;
import com.anmay.spendwise.dto.Responses.TransactionView;
import com.anmay.spendwise.security.CurrentUserService;
import com.anmay.spendwise.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final CurrentUserService currentUserService;

    public TransactionController(TransactionService transactionService,
                                 CurrentUserService currentUserService) {
        this.transactionService = transactionService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<TransactionView> list() {
        return transactionService.list(currentUserService.currentUserId());
    }

    @PatchMapping("/{transactionId}/category")
    public TransactionView updateCategory(@PathVariable Long transactionId,
                                          @Valid @RequestBody UpdateCategoryRequest request) {
        return transactionService.updateCategory(
                currentUserService.currentUserId(), transactionId, request.categoryId());
    }
}
