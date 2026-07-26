package com.anmay.spendwise.config;

import com.anmay.spendwise.entity.*;
import com.anmay.spendwise.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Configuration
public class DemoDataSeeder {

    @Bean
    CommandLineRunner seedDemoData(AppUserRepository users,
                                   WalletRepository wallets,
                                   CategoryRepository categories,
                                   BudgetRepository budgets,
                                   ExpenseTransactionRepository transactions) {
        return args -> {
            if (users.count() > 0) return;

            AppUser user = users.save(new AppUser("Anmay", "demo@spendwise.local"));
            wallets.save(new Wallet(user, new BigDecimal("24500.00")));

            Map<String, Category> c = new HashMap<>();
            addCategory(c, categories, user, "Food", "🍜");
            addCategory(c, categories, user, "Entertainment", "🎬");
            addCategory(c, categories, user, "Shopping", "🛍️");
            addCategory(c, categories, user, "Travel", "🚕");
            addCategory(c, categories, user, "Family", "👨‍👩‍👧");
            addCategory(c, categories, user, "Gifts", "🎁");
            addCategory(c, categories, user, "Bills", "🧾");
            addCategory(c, categories, user, "Healthcare", "🩺");
            addCategory(c, categories, user, "Education", "📚");
            addCategory(c, categories, user, "Investment", "📈");
            addCategory(c, categories, user, "Other", "🏷️");

            budgets.save(new Budget(user, c.get("Entertainment"), new BigDecimal("5000"), 80));
            budgets.save(new Budget(user, c.get("Food"), new BigDecimal("6000"), 80));
            budgets.save(new Budget(user, c.get("Shopping"), new BigDecimal("4500"), 75));
            budgets.save(new Budget(user, c.get("Travel"), new BigDecimal("3500"), 80));

            LocalDateTime now = LocalDateTime.now();
            saveTx(transactions, user, c.get("Entertainment"), "PVR Cinemas", "Movie tickets", "650", now.minusDays(1));
            saveTx(transactions, user, c.get("Food"), "Swiggy", "Dinner order", "420", now.minusDays(2));
            saveTx(transactions, user, c.get("Travel"), "Uber", "Office ride", "280", now.minusDays(3));
            saveTx(transactions, user, c.get("Shopping"), "Myntra", "Clothing", "1299", now.minusDays(4));
            saveTx(transactions, user, c.get("Bills"), "Airtel", "Mobile recharge", "399", now.minusDays(5));
            saveTx(transactions, user, c.get("Food"), "D-Mart", "Groceries", "1820", now.minusDays(7));
            saveTx(transactions, user, c.get("Investment"), "Zerodha", "Monthly SIP", "2000", now.minusDays(9));
            saveTx(transactions, user, c.get("Entertainment"), "Spotify", "Subscription", "119", now.minusDays(11));

            LocalDateTime previousMonth = now.minusMonths(1);
            saveTx(transactions, user, c.get("Food"), "Swiggy", "Lunch", "350", previousMonth.minusDays(2));
            saveTx(transactions, user, c.get("Shopping"), "Amazon", "Household item", "999", previousMonth.minusDays(4));
            saveTx(transactions, user, c.get("Travel"), "Indian Railways", "Train ticket", "1450", previousMonth.minusDays(6));
        };
    }

    private void addCategory(Map<String, Category> map, CategoryRepository repository,
                             AppUser user, String name, String icon) {
        map.put(name, repository.save(new Category(user, name, icon, true)));
    }

    private void saveTx(ExpenseTransactionRepository repository, AppUser user, Category category,
                        String merchant, String description, String amount, LocalDateTime date) {
        repository.save(new ExpenseTransaction(
                user, merchant, description, new BigDecimal(amount), category,
                category.getName(), 0.95, PaymentStatus.SUCCESSFUL, date));
    }
}
