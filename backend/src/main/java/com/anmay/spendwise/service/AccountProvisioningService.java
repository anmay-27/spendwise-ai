package com.anmay.spendwise.service;

import com.anmay.spendwise.entity.AppUser;
import com.anmay.spendwise.entity.Budget;
import com.anmay.spendwise.entity.Category;
import com.anmay.spendwise.entity.Wallet;
import com.anmay.spendwise.repository.BudgetRepository;
import com.anmay.spendwise.repository.CategoryRepository;
import com.anmay.spendwise.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountProvisioningService {
  private static final Map<String, String> DEFAULT_CATEGORIES = new LinkedHashMap<>();

  static {
    DEFAULT_CATEGORIES.put("Food", "🍜");
    DEFAULT_CATEGORIES.put("Entertainment", "🎬");
    DEFAULT_CATEGORIES.put("Shopping", "🛍️");
    DEFAULT_CATEGORIES.put("Travel", "🚕");
    DEFAULT_CATEGORIES.put("Family", "👨‍👩‍👧");
    DEFAULT_CATEGORIES.put("Gifts", "🎁");
    DEFAULT_CATEGORIES.put("Bills", "🧾");
    DEFAULT_CATEGORIES.put("Healthcare", "🩺");
    DEFAULT_CATEGORIES.put("Education", "📚");
    DEFAULT_CATEGORIES.put("Investment", "📈");
    DEFAULT_CATEGORIES.put("Other", "🏷️");
  }

  private final WalletRepository walletRepository;
  private final CategoryRepository categoryRepository;
  private final BudgetRepository budgetRepository;
  private final BigDecimal initialWalletBalance;

  public AccountProvisioningService(
      WalletRepository walletRepository,
      CategoryRepository categoryRepository,
      BudgetRepository budgetRepository,
      @Value("${app.demo-initial-wallet-balance:50000}") BigDecimal initialWalletBalance) {
    this.walletRepository = walletRepository;
    this.categoryRepository = categoryRepository;
    this.budgetRepository = budgetRepository;
    this.initialWalletBalance = initialWalletBalance;
  }

  @Transactional
  public void provision(AppUser user) {
    provision(user, initialWalletBalance);
  }

  @Transactional
  public void provision(AppUser user, BigDecimal walletBalance) {
    walletRepository
        .findByUserId(user.getId())
        .orElseGet(() -> walletRepository.save(new Wallet(user, walletBalance)));

    Map<String, Category> categories = new LinkedHashMap<>();
    categoryRepository
        .findByUserIdOrderByNameAsc(user.getId())
        .forEach(category -> categories.put(category.getName(), category));

    DEFAULT_CATEGORIES.forEach(
        (name, icon) ->
            categories.computeIfAbsent(
                name, ignored -> categoryRepository.save(new Category(user, name, icon, true))));

    createBudgetIfMissing(user, categories.get("Entertainment"), "5000", 80);
    createBudgetIfMissing(user, categories.get("Food"), "6000", 80);
    createBudgetIfMissing(user, categories.get("Shopping"), "4500", 75);
    createBudgetIfMissing(user, categories.get("Travel"), "3500", 80);
  }

  private void createBudgetIfMissing(
      AppUser user, Category category, String limit, int warningPercent) {
    budgetRepository
        .findByUserIdAndCategoryIdAndMonth(
            user.getId(), category.getId(), java.time.YearMonth.now().toString())
        .orElseGet(
            () ->
                budgetRepository.save(
                    new Budget(user, category, new BigDecimal(limit), warningPercent)));
  }
}
