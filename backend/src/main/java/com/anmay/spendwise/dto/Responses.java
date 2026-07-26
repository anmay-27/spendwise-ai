package com.anmay.spendwise.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class Responses {

    private Responses() {
    }

    public record AuthUserResponse(
            Long id,
            String name,
            String email,
            String role,
            boolean googleConnected
    ) {
    }

    public record CsrfResponse(String token, String headerName) {
    }

    public record CategoryView(
            Long id,
            String name,
            String icon,
            boolean systemDefined
    ) {
    }

    public record TransactionView(
            Long id,
            String merchant,
            String description,
            BigDecimal amount,
            Long categoryId,
            String category,
            String categoryIcon,
            String aiSuggestedCategory,
            double aiConfidence,
            String status,
            LocalDateTime occurredAt
    ) {
    }

    public record BudgetProgress(
            Long budgetId,
            Long categoryId,
            String category,
            String icon,
            BigDecimal spent,
            BigDecimal limit,
            int warningPercent,
            double usedPercent,
            BigDecimal projectedSpend,
            boolean warning
    ) {
    }

    public record CategorySpend(
            String category,
            String icon,
            BigDecimal amount,
            double percentage
    ) {
    }

    public record DashboardResponse(
            Long userId,
            String userName,
            BigDecimal walletBalance,
            BigDecimal totalSpentThisMonth,
            BigDecimal totalBudget,
            BigDecimal remainingBudget,
            List<CategorySpend> categorySpending,
            List<BudgetProgress> budgets,
            List<TransactionView> recentTransactions
    ) {
    }

    public record PaymentPreviewResponse(
            String merchant,
            BigDecimal amount,
            String description,
            Long suggestedCategoryId,
            String suggestedCategory,
            String suggestedCategoryIcon,
            double confidence,
            String predictionSource
    ) {
    }

    public record PaymentResponse(
            Long transactionId,
            BigDecimal amount,
            String merchant,
            Long categoryId,
            String category,
            String categoryIcon,
            String aiSuggestedCategory,
            double confidence,
            BigDecimal walletBalance,
            String budgetWarning
    ) {
    }

    public record MonthlyReportResponse(
            int year,
            int month,
            BigDecimal totalSpent,
            BigDecimal previousMonthSpent,
            double changePercent,
            String highestCategory,
            BigDecimal highestCategoryAmount,
            List<CategorySpend> categorySpending,
            List<BudgetProgress> budgets,
            List<String> insights
    ) {
    }

    public record AssistantResponse(
            String answer,
            List<String> facts
    ) {
    }
}
