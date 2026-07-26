package com.anmay.spendwise.service;

import com.anmay.spendwise.dto.Requests.ConfirmPaymentRequest;
import com.anmay.spendwise.dto.Requests.PaymentPreviewRequest;
import com.anmay.spendwise.dto.Responses.PaymentPreviewResponse;
import com.anmay.spendwise.dto.Responses.PaymentResponse;
import com.anmay.spendwise.entity.AppUser;
import com.anmay.spendwise.entity.Category;
import com.anmay.spendwise.entity.ExpenseTransaction;
import com.anmay.spendwise.entity.PaymentStatus;
import com.anmay.spendwise.entity.Wallet;
import com.anmay.spendwise.repository.AppUserRepository;
import com.anmay.spendwise.repository.BudgetRepository;
import com.anmay.spendwise.repository.CategoryRepository;
import com.anmay.spendwise.repository.ExpenseTransactionRepository;
import com.anmay.spendwise.repository.WalletRepository;
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

    @Transactional(readOnly = true)
    public PaymentPreviewResponse preview(Long userId, PaymentPreviewRequest request) {
        requireUser(userId);
        List<Category> categories = categoriesFor(userId);
        MlServiceClient.Prediction prediction = predict(
                userId,
                request.merchant(),
                request.description(),
                request.amount(),
                categories
        );
        Category suggestedCategory = resolvePredictedCategory(userId, prediction.category());

        return new PaymentPreviewResponse(
                request.merchant().trim(),
                request.amount(),
                normalize(request.description()),
                suggestedCategory.getId(),
                suggestedCategory.getName(),
                suggestedCategory.getIcon(),
                prediction.confidence(),
                prediction.model()
        );
    }

    @Transactional
    public PaymentResponse confirm(Long userId, ConfirmPaymentRequest request) {
        AppUser user = requireUser(userId);
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        if (wallet.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalArgumentException("Insufficient demo wallet balance");
        }

        Category finalCategory = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Selected category not found"));
        if (!finalCategory.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Selected category does not belong to this user");
        }

        List<Category> categories = categoriesFor(userId);
        MlServiceClient.Prediction prediction = predict(
                userId,
                request.merchant(),
                request.description(),
                request.amount(),
                categories
        );

        ExpenseTransaction transaction = transactionRepository.save(new ExpenseTransaction(
                user,
                request.merchant().trim(),
                normalize(request.description()),
                request.amount(),
                finalCategory,
                prediction.category(),
                prediction.confidence(),
                PaymentStatus.SUCCESSFUL,
                LocalDateTime.now()
        ));

        wallet.setBalance(wallet.getBalance().subtract(request.amount()));
        walletRepository.save(wallet);

        mlServiceClient.sendFeedback(
                userId,
                request.merchant(),
                normalize(request.description()),
                finalCategory.getName()
        );

        return new PaymentResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getMerchantName(),
                finalCategory.getId(),
                finalCategory.getName(),
                finalCategory.getIcon(),
                transaction.getAiSuggestedCategory(),
                transaction.getAiConfidence(),
                wallet.getBalance(),
                budgetWarning(userId, finalCategory)
        );
    }

    private AppUser requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private List<Category> categoriesFor(Long userId) {
        List<Category> categories = categoryRepository.findByUserIdOrderByNameAsc(userId);
        if (categories.isEmpty()) {
            throw new IllegalArgumentException("No categories are configured for this user");
        }
        return categories;
    }

    private MlServiceClient.Prediction predict(Long userId,
                                                String merchant,
                                                String description,
                                                BigDecimal amount,
                                                List<Category> categories) {
        return mlServiceClient.predict(
                userId,
                merchant,
                normalize(description),
                amount.doubleValue(),
                categories.stream().map(Category::getName).toList()
        );
    }

    private Category resolvePredictedCategory(Long userId, String predictedCategory) {
        return categoryRepository.findByUserIdAndNameIgnoreCase(userId, predictedCategory)
                .orElseGet(() -> categoryRepository
                        .findByUserIdAndNameIgnoreCase(userId, "Other")
                        .orElseThrow(() -> new IllegalArgumentException("The Other category is missing")));
    }

    private String budgetWarning(Long userId, Category category) {
        return budgetRepository.findByUserIdAndCategoryId(userId, category.getId())
                .map(budget -> {
                    BigDecimal spent = analyticsService.total(
                            analyticsService.transactionsForMonth(userId, YearMonth.now()).stream()
                                    .filter(transaction -> transaction.getCategory().getId()
                                            .equals(category.getId()))
                                    .toList()
                    );
                    double percent = spent
                            .divide(budget.getMonthlyLimit(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue();

                    if (percent >= 100) {
                        return category.getName() + " budget exceeded: " + Math.round(percent) + "% used.";
                    }
                    if (percent >= budget.getWarningPercent()) {
                        return category.getName() + " has reached " + Math.round(percent) + "% of its budget.";
                    }
                    return null;
                })
                .orElse(null);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
