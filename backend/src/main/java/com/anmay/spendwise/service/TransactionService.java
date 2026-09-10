package com.anmay.spendwise.service;

import com.anmay.spendwise.dto.Responses.TransactionView;
import com.anmay.spendwise.entity.Category;
import com.anmay.spendwise.entity.ExpenseTransaction;
import com.anmay.spendwise.repository.CategoryRepository;
import com.anmay.spendwise.repository.ExpenseTransactionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {
  @org.springframework.beans.factory.annotation.Autowired
  private com.anmay.spendwise.events.EventOutbox events;

  @org.springframework.beans.factory.annotation.Autowired
  private com.anmay.spendwise.security.CurrentUserService currentUser;

  private final ExpenseTransactionRepository transactionRepository;
  private final CategoryRepository categoryRepository;
  private final MlServiceClient mlServiceClient;

  public TransactionService(
      ExpenseTransactionRepository transactionRepository,
      CategoryRepository categoryRepository,
      MlServiceClient mlServiceClient) {
    this.transactionRepository = transactionRepository;
    this.categoryRepository = categoryRepository;
    this.mlServiceClient = mlServiceClient;
  }

  @Transactional(readOnly = true)
  public List<TransactionView> list(Long userId) {
    return transactionRepository.findByUserIdOrderByOccurredAtDesc(userId).stream()
        .map(ViewMapper::transaction)
        .toList();
  }

  @Transactional
  public TransactionView updateCategory(Long transactionId, Long categoryId) {
    ExpenseTransaction tx =
        transactionRepository
            .findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
    Category category =
        categoryRepository
            .findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    if (!tx.getUser().getId().equals(currentUser.currentUserId())) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.NOT_FOUND, "Transaction not found");
    }
    if (!tx.getUser().getId().equals(category.getUser().getId())) {
      throw new IllegalArgumentException("Category does not belong to this user");
    }
    tx.setCategory(category);
    ExpenseTransaction saved = transactionRepository.saveAndFlush(tx);
    events.append(
        "TRANSACTION_UPDATED",
        tx.getId(),
        tx.getUser().getId(),
        java.util.Map.of("categoryCorrection", true));
    return ViewMapper.transaction(saved);
  }
}
