package com.anmay.spendwise.controller;

import com.anmay.spendwise.dto.Requests.UpdateCategoryRequest;
import com.anmay.spendwise.dto.Responses.TransactionView;
import com.anmay.spendwise.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    public TransactionController(TransactionService transactionService) { this.transactionService = transactionService; }

    @GetMapping
    public List<TransactionView> list(@RequestParam(defaultValue = "1") Long userId) {
        return transactionService.list(userId);
    }

    @PatchMapping("/{transactionId}/category")
    public TransactionView updateCategory(@PathVariable Long transactionId,
                                          @Valid @RequestBody UpdateCategoryRequest request) {
        return transactionService.updateCategory(transactionId, request.categoryId());
    }
}
