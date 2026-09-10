package com.anmay.spendwise.config;

import com.anmay.spendwise.entity.*;
import com.anmay.spendwise.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "app.demo.enabled",
    havingValue = "true")
public class DemoDataSeeder {

  @Bean
  CommandLineRunner seedDemoData(
      AppUserRepository users,
      WalletRepository wallets,
      CategoryRepository categories,
      BudgetRepository budgets,
      ExpenseTransactionRepository transactions,
      com.anmay.spendwise.events.EventOutbox outbox,
      org.springframework.transaction.PlatformTransactionManager manager,
      PasswordEncoder passwordEncoder,
      @org.springframework.beans.factory.annotation.Value("${app.demo.password}")
          String demoPassword) {
    return args -> {
      if (users.count() > 0) return;
      if (demoPassword.length() < 12)
        throw new IllegalStateException("DEMO_PASSWORD requires at least 12 characters");
      new org.springframework.transaction.support.TransactionTemplate(manager)
          .executeWithoutResult(
              status -> {
                AppUser user =
                    users.save(
                        new AppUser(
                            "Anmay", "demo@spendwise.local", passwordEncoder.encode(demoPassword)));
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
                saveTx(
                    transactions,
                    user,
                    c.get("Entertainment"),
                    "PVR Cinemas",
                    "Movie tickets",
                    "650",
                    now.minusDays(1));
                saveTx(
                    transactions,
                    user,
                    c.get("Food"),
                    "Swiggy",
                    "Dinner order",
                    "420",
                    now.minusDays(2));
                saveTx(
                    transactions,
                    user,
                    c.get("Travel"),
                    "Uber",
                    "Office ride",
                    "280",
                    now.minusDays(3));
                saveTx(
                    transactions,
                    user,
                    c.get("Shopping"),
                    "Myntra",
                    "Clothing",
                    "1299",
                    now.minusDays(4));
                saveTx(
                    transactions,
                    user,
                    c.get("Bills"),
                    "Airtel",
                    "Mobile recharge",
                    "399",
                    now.minusDays(5));
                saveTx(
                    transactions,
                    user,
                    c.get("Food"),
                    "D-Mart",
                    "Groceries",
                    "1820",
                    now.minusDays(7));
                saveTx(
                    transactions,
                    user,
                    c.get("Investment"),
                    "Zerodha",
                    "Monthly SIP",
                    "2000",
                    now.minusDays(9));
                saveTx(
                    transactions,
                    user,
                    c.get("Entertainment"),
                    "Spotify",
                    "Subscription",
                    "119",
                    now.minusDays(11));

                LocalDateTime previousMonth = now.minusMonths(1);
                saveTx(
                    transactions,
                    user,
                    c.get("Food"),
                    "Swiggy",
                    "Lunch",
                    "350",
                    previousMonth.minusDays(2));
                saveTx(
                    transactions,
                    user,
                    c.get("Shopping"),
                    "Amazon",
                    "Household item",
                    "999",
                    previousMonth.minusDays(4));
                saveTx(
                    transactions,
                    user,
                    c.get("Travel"),
                    "Indian Railways",
                    "Train ticket",
                    "1450",
                    previousMonth.minusDays(6));
                for (int months = 0; months < 6; months++) {
                  var base = java.time.YearMonth.now().minusMonths(months).atDay(1).atTime(9, 0);
                  var salary =
                      new ExpenseTransaction(
                          user,
                          "Monthly salary",
                          "Demo salary",
                          new BigDecimal("65000"),
                          c.get("Other"),
                          "Other",
                          1,
                          PaymentStatus.SUCCESSFUL,
                          base);
                  salary.revise(
                      "Monthly salary",
                      "Demo salary",
                      new BigDecimal("65000"),
                      c.get("Other"),
                      TransactionType.INCOME,
                      base,
                      false);
                  transactions.save(salary);
                  saveTx(
                      transactions,
                      user,
                      c.get("Entertainment"),
                      "Netflix",
                      "Monthly subscription",
                      "649",
                      base);
                  for (int day = 2; day <= 20; day += 3) {
                    var when = base.withDayOfMonth(day);
                    if (when.isBefore(now))
                      saveTx(
                          transactions,
                          user,
                          c.get("Food"),
                          "Swiggy",
                          "Food delivery",
                          String.valueOf(350 + months * 20 + day * 5),
                          when);
                  }
                  for (int day = 2; day <= 8; day += 2) {
                    var when = base.withDayOfMonth(day);
                    if (when.isBefore(now))
                      saveTx(
                          transactions,
                          user,
                          c.get("Shopping"),
                          "Local store",
                          "Household purchase",
                          "1200",
                          when);
                  }
                }
                saveTx(
                    transactions,
                    user,
                    c.get("Shopping"),
                    "Laptop store",
                    "Unusual purchase example",
                    "42000",
                    now.minusHours(1));
                transactions.flush();
                outbox.append(
                    "TRANSACTION_CREATED",
                    null,
                    user.getId(),
                    java.util.Map.of("source", "demo-seed"));
              });
    };
  }

  private void addCategory(
      Map<String, Category> map,
      CategoryRepository repository,
      AppUser user,
      String name,
      String icon) {
    map.put(name, repository.save(new Category(user, name, icon, true)));
  }

  private void saveTx(
      ExpenseTransactionRepository repository,
      AppUser user,
      Category category,
      String merchant,
      String description,
      String amount,
      LocalDateTime date) {
    repository.save(
        new ExpenseTransaction(
            user,
            merchant,
            description,
            new BigDecimal(amount),
            category,
            category.getName(),
            0.95,
            PaymentStatus.SUCCESSFUL,
            date));
  }
}
