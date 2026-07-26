package com.anmay.spendwise.service;

import com.anmay.spendwise.dto.Responses.TransactionView;
import com.anmay.spendwise.entity.Category;
import com.anmay.spendwise.entity.ExpenseTransaction;
import com.anmay.spendwise.repository.CategoryRepository;
import com.anmay.spendwise.repository.ExpenseTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransactionService {
    private final ExpenseTransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final MlServiceClient mlServiceClient;

    public TransactionService(ExpenseTransactionRepository transactionRepository,
                              CategoryRepository categoryRepository,
                              MlServiceClient mlServiceClient) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.mlServiceClient = mlServiceClient;
    }

    @Transactional(readOnly = true)
    public List<TransactionView> list(Long userId) {
        return transactionRepository.findByUserIdOrderByOccurredAtDesc(userId)
                .stream()
                .map(ViewMapper::transaction)
                .toList();
    }

    @Transactional
    public TransactionView updateCategory(Long userId, Long transactionId, Long categoryId) {
        ExpenseTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        if (!transaction.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Transaction does not belong to this user");
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        if (!category.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Category does not belong to this user");
        }

        transaction.setCategory(category);
        ExpenseTransaction saved = transactionRepository.save(transaction);
        mlServiceClient.sendFeedback(
                userId,
                transaction.getMerchantName(),
                transaction.getDescription(),
                category.getName()
        );
        return ViewMapper.transaction(saved);
    }
}
