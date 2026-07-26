package com.anmay.spendwise.service;

import com.anmay.spendwise.dto.Requests.PaymentRequest;
import com.anmay.spendwise.dto.Responses.PaymentResponse;
import com.anmay.spendwise.entity.*;
import com.anmay.spendwise.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
public class PaymentService {
    private final AppUserRepository userRepository;
    private final WalletRepository walletRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseTransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final MlServiceClient mlServiceClient;
    private final AnalyticsService analyticsService;

    public PaymentService(AppUserRepository userRepository,
                          WalletRepository walletRepository,
                          CategoryRepository categoryRepository,
                          ExpenseTransactionRepository transactionRepository,
                          BudgetRepository budgetRepository,
                          MlServiceClient mlServiceClient,
                          AnalyticsService analyticsService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.mlServiceClient = mlServiceClient;
        this.analyticsService = analyticsService;
    }

    @Transactional
    public PaymentResponse pay(PaymentRequest request) {
        AppUser user = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Wallet wallet = walletRepository.findByUserId(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        if (wallet.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalArgumentException("Insufficient demo wallet balance");
        }

        List<Category> categories = categoryRepository.findByUserIdOrderByNameAsc(request.userId());
        List<String> names = categories.stream().map(Category::getName).toList();
        MlServiceClient.Prediction prediction = mlServiceClient.predict(
                request.userId(), request.merchant(), request.description(),
                request.amount().doubleValue(), names);

        Category category = categoryRepository
                .findByUserIdAndNameIgnoreCase(request.userId(), prediction.category())
                .orElseGet(() -> categoryRepository
                        .findByUserIdAndNameIgnoreCase(request.userId(), "Other")
                        .orElseThrow(() -> new IllegalArgumentException("Other category is missing")));

        ExpenseTransaction transaction = new ExpenseTransaction(
                user, request.merchant().trim(), normalize(request.description()), request.amount(),
                category, prediction.category(), prediction.confidence(), PaymentStatus.SUCCESSFUL,
                LocalDateTime.now());
        transactionRepository.save(transaction);

        wallet.setBalance(wallet.getBalance().subtract(request.amount()));
        walletRepository.save(wallet);

        String warning = budgetWarning(request.userId(), category);
        return new PaymentResponse(
                transaction.getId(), transaction.getAmount(), transaction.getMerchantName(),
                category.getId(), category.getName(), category.getIcon(),
                transaction.getAiSuggestedCategory(), transaction.getAiConfidence(),
                wallet.getBalance(), warning);
    }

    private String budgetWarning(Long userId, Category category) {
        return budgetRepository.findByUserIdAndCategoryId(userId, category.getId())
                .map(budget -> {
                    BigDecimal spent = analyticsService.total(
                            analyticsService.transactionsForMonth(userId, YearMonth.now()).stream()
                                    .filter(tx -> tx.getCategory().getId().equals(category.getId()))
                                    .toList());
                    double percent = spent.divide(budget.getMonthlyLimit(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue();
                    if (percent >= 100) return category.getName() + " budget exceeded: " + Math.round(percent) + "% used.";
                    if (percent >= budget.getWarningPercent()) return category.getName() + " has reached " + Math.round(percent) + "% of its budget.";
                    return null;
                }).orElse(null);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return "";
        return value.trim();
    }
}
