package com.anmay.spendwise.config;

import com.anmay.spendwise.entity.*;
import com.anmay.spendwise.repository.AppUserRepository;
import com.anmay.spendwise.repository.CategoryRepository;
import com.anmay.spendwise.repository.ExpenseTransactionRepository;
import com.anmay.spendwise.service.AccountProvisioningService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class DemoDataSeeder {

    @Bean
    CommandLineRunner seedDemoData(AppUserRepository users,
                                   CategoryRepository categories,
                                   ExpenseTransactionRepository transactions,
                                   PasswordEncoder passwordEncoder,
                                   AccountProvisioningService provisioningService) {
        return args -> {
            if (users.count() > 0) {
                return;
            }

            AppUser user = users.save(new AppUser(
                    "Anmay",
                    "demo@spendwise.local",
                    passwordEncoder.encode("Demo@123")
            ));
            provisioningService.provision(user, new BigDecimal("24500.00"));

            Map<String, Category> categoryMap = categories.findByUserIdOrderByNameAsc(user.getId())
                    .stream()
                    .collect(Collectors.toMap(Category::getName, category -> category));

            LocalDateTime now = LocalDateTime.now();
            saveTx(transactions, user, categoryMap.get("Entertainment"), "PVR Cinemas", "Movie tickets", "650", now.minusDays(1));
            saveTx(transactions, user, categoryMap.get("Food"), "Swiggy", "Dinner order", "420", now.minusDays(2));
            saveTx(transactions, user, categoryMap.get("Travel"), "Uber", "Office ride", "280", now.minusDays(3));
            saveTx(transactions, user, categoryMap.get("Shopping"), "Myntra", "Clothing", "1299", now.minusDays(4));
            saveTx(transactions, user, categoryMap.get("Bills"), "Airtel", "Mobile recharge", "399", now.minusDays(5));
            saveTx(transactions, user, categoryMap.get("Food"), "D-Mart", "Groceries", "1820", now.minusDays(7));
            saveTx(transactions, user, categoryMap.get("Investment"), "Zerodha", "Monthly SIP", "2000", now.minusDays(9));
            saveTx(transactions, user, categoryMap.get("Entertainment"), "Spotify", "Subscription", "119", now.minusDays(11));

            LocalDateTime previousMonth = now.minusMonths(1);
            saveTx(transactions, user, categoryMap.get("Food"), "Swiggy", "Lunch", "350", previousMonth.minusDays(2));
            saveTx(transactions, user, categoryMap.get("Shopping"), "Amazon", "Household item", "999", previousMonth.minusDays(4));
            saveTx(transactions, user, categoryMap.get("Travel"), "Indian Railways", "Train ticket", "1450", previousMonth.minusDays(6));
        };
    }

    private void saveTx(ExpenseTransactionRepository repository,
                        AppUser user,
                        Category category,
                        String merchant,
                        String description,
                        String amount,
                        LocalDateTime date) {
        repository.save(new ExpenseTransaction(
                user,
                merchant,
                description,
                new BigDecimal(amount),
                category,
                category.getName(),
                0.95,
                PaymentStatus.SUCCESSFUL,
                date
        ));
    }
}
