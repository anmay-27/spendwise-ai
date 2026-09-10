package com.anmay.spendwise.controller;

import com.anmay.spendwise.dto.Requests.UpdateCategoryRequest;
import com.anmay.spendwise.dto.Responses.TransactionView;
import com.anmay.spendwise.service.TransactionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
  @org.springframework.beans.factory.annotation.Autowired
  private com.anmay.spendwise.security.CurrentUserService currentUser;

  private final TransactionService transactionService;

  public TransactionController(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @GetMapping
  public List<TransactionView> list(@RequestParam(required = false) Long ignoredUserId) {
    return transactionService.list(currentUser.currentUserId());
  }

  @PatchMapping("/{transactionId}/category")
  public TransactionView updateCategory(
      @PathVariable Long transactionId, @Valid @RequestBody UpdateCategoryRequest request) {
    return transactionService.updateCategory(transactionId, request.categoryId());
  }
}
