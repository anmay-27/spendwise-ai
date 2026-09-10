package com.anmay.spendwise.service;

import com.anmay.spendwise.dto.Responses.*;
import com.anmay.spendwise.entity.*;
import com.anmay.spendwise.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {
  private final AppUserRepository userRepository;
  private final WalletRepository walletRepository;
  private final ExpenseTransactionRepository transactionRepository;
  private final BudgetRepository budgetRepository;

  public AnalyticsService(
      AppUserRepository userRepository,
      WalletRepository walletRepository,
      ExpenseTransactionRepository transactionRepository,
      BudgetRepository budgetRepository) {
    this.userRepository = userRepository;
    this.walletRepository = walletRepository;
    this.transactionRepository = transactionRepository;
    this.budgetRepository = budgetRepository;
  }

  public DashboardResponse dashboard(Long userId) {
    AppUser user = requireUser(userId);
    Wallet wallet =
        walletRepository
            .findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
    YearMonth current = YearMonth.now();
    List<ExpenseTransaction> transactions = transactionsForMonth(userId, current);
    List<Budget> budgets =
        budgetRepository.findByUserIdAndMonthOrderByCategoryNameAsc(
            userId, YearMonth.now().toString());

    BigDecimal totalSpent = total(transactions);
    BigDecimal totalBudget =
        budgets.stream().map(Budget::getMonthlyLimit).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal remaining = totalBudget.subtract(totalSpent).max(BigDecimal.ZERO);

    List<TransactionView> recent =
        transactionRepository.findByUserIdOrderByOccurredAtDesc(userId).stream()
            .limit(6)
            .map(ViewMapper::transaction)
            .toList();

    return new DashboardResponse(
        userId,
        user.getName(),
        wallet.getBalance(),
        totalSpent,
        totalBudget,
        remaining,
        categorySpending(transactions),
        budgetProgress(budgets, transactions, current),
        recent);
  }

  public MonthlyReportResponse monthlyReport(Long userId, Integer year, Integer month) {
    requireUser(userId);
    YearMonth target = year == null || month == null ? YearMonth.now() : YearMonth.of(year, month);
    List<ExpenseTransaction> currentTransactions = transactionsForMonth(userId, target);
    List<ExpenseTransaction> previousTransactions =
        transactionsForMonth(userId, target.minusMonths(1));
    List<Budget> budgets =
        budgetRepository.findByUserIdAndMonthOrderByCategoryNameAsc(userId, target.toString());

    BigDecimal currentTotal = total(currentTransactions);
    BigDecimal previousTotal = total(previousTransactions);
    double change =
        previousTotal.signum() == 0
            ? (currentTotal.signum() == 0 ? 0 : 100)
            : currentTotal
                .subtract(previousTotal)
                .divide(previousTotal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

    List<CategorySpend> spending = categorySpending(currentTransactions);
    CategorySpend highest =
        spending.isEmpty()
            ? new CategorySpend("No spending", "—", BigDecimal.ZERO, 0)
            : spending.get(0);
    List<BudgetProgress> progress = budgetProgress(budgets, currentTransactions, target);

    List<String> insights = new ArrayList<>();
    if (change > 0)
      insights.add(
          "Spending increased by " + round(change) + "% compared with the previous month.");
    else if (change < 0)
      insights.add(
          "Spending decreased by "
              + round(Math.abs(change))
              + "% compared with the previous month.");
    else insights.add("Spending is unchanged compared with the previous month.");
    if (!spending.isEmpty())
      insights.add(
          highest.category()
              + " was the highest category at ₹"
              + highest.amount().setScale(0, RoundingMode.HALF_UP)
              + ".");
    progress.stream()
        .filter(BudgetProgress::warning)
        .findFirst()
        .ifPresent(
            p ->
                insights.add(
                    p.category()
                        + " has reached "
                        + round(p.usedPercent())
                        + "% of its monthly limit."));
    if (progress.stream().noneMatch(BudgetProgress::warning))
      insights.add("All configured categories are currently below their warning levels.");

    return new MonthlyReportResponse(
        target.getYear(),
        target.getMonthValue(),
        currentTotal,
        previousTotal,
        round(change),
        highest.category(),
        highest.amount(),
        spending,
        progress,
        insights);
  }

  public List<BudgetProgress> getBudgetProgress(Long userId) {
    return getBudgetProgress(userId, YearMonth.now());
  }

  public List<BudgetProgress> getBudgetProgress(Long userId, YearMonth current) {
    requireUser(userId);
    return budgetProgress(
        budgetRepository.findByUserIdAndMonthOrderByCategoryNameAsc(userId, current.toString()),
        transactionsForMonth(userId, current),
        current);
  }

  public List<ExpenseTransaction> transactionsForMonth(Long userId, YearMonth month) {
    LocalDateTime start = month.atDay(1).atStartOfDay();
    LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
    return transactionRepository
        .findByUserIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
            userId, start, end);
  }

  public List<CategorySpend> categorySpending(List<ExpenseTransaction> transactions) {
    BigDecimal total = total(transactions);
    Map<Category, BigDecimal> grouped = new HashMap<>();
    for (ExpenseTransaction tx : transactions) {
      if (tx.getType() != TransactionType.EXPENSE) continue;
      grouped.merge(tx.getCategory(), tx.getAmount(), BigDecimal::add);
    }
    return grouped.entrySet().stream()
        .map(
            entry ->
                new CategorySpend(
                    entry.getKey().getName(),
                    entry.getKey().getIcon(),
                    entry.getValue(),
                    total.signum() == 0
                        ? 0
                        : round(
                            entry
                                .getValue()
                                .divide(total, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .doubleValue())))
        .sorted(Comparator.comparing(CategorySpend::amount).reversed())
        .toList();
  }

  private List<BudgetProgress> budgetProgress(
      List<Budget> budgets, List<ExpenseTransaction> transactions, YearMonth month) {
    Map<Long, BigDecimal> spentByCategory = new HashMap<>();
    for (ExpenseTransaction tx : transactions) {
      if (tx.getType() != TransactionType.EXPENSE) continue;
      spentByCategory.merge(tx.getCategory().getId(), tx.getAmount(), BigDecimal::add);
    }
    int daysInMonth = month.lengthOfMonth();
    int elapsedDays = month.equals(YearMonth.now()) ? LocalDate.now().getDayOfMonth() : daysInMonth;

    return budgets.stream()
        .map(
            budget -> {
              BigDecimal spent =
                  spentByCategory.getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO);
              double usedPercent =
                  budget.getMonthlyLimit().signum() == 0
                      ? 0
                      : spent
                          .divide(budget.getMonthlyLimit(), 4, RoundingMode.HALF_UP)
                          .multiply(BigDecimal.valueOf(100))
                          .doubleValue();
              BigDecimal projected =
                  elapsedDays == 0
                      ? spent
                      : spent
                          .multiply(BigDecimal.valueOf(daysInMonth))
                          .divide(BigDecimal.valueOf(elapsedDays), 2, RoundingMode.HALF_UP);
              return new BudgetProgress(
                  budget.getId(),
                  budget.getCategory().getId(),
                  budget.getCategory().getName(),
                  budget.getCategory().getIcon(),
                  spent,
                  budget.getMonthlyLimit(),
                  budget.getWarningPercent(),
                  round(usedPercent),
                  projected,
                  usedPercent >= budget.getWarningPercent());
            })
        .toList();
  }

  public BigDecimal total(List<ExpenseTransaction> transactions) {
    return transactions.stream()
        .filter(tx -> tx.getType() == TransactionType.EXPENSE)
        .map(ExpenseTransaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private AppUser requireUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
  }

  private double round(double value) {
    return Math.round(value * 10.0) / 10.0;
  }
}
