package com.anmay.spendwise.service;

import com.anmay.spendwise.dto.Responses.AssistantResponse;
import com.anmay.spendwise.dto.Responses.CategorySpend;
import com.anmay.spendwise.entity.ExpenseTransaction;
import com.anmay.spendwise.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.regex.*;

@Service
public class AssistantService {
    private final AnalyticsService analyticsService;
    private final CategoryRepository categoryRepository;

    public AssistantService(AnalyticsService analyticsService,
                            CategoryRepository categoryRepository) {
        this.analyticsService = analyticsService;
        this.categoryRepository = categoryRepository;
    }

    public AssistantResponse answer(Long userId, String question) {
        String q = question.toLowerCase(Locale.ROOT).trim();
        YearMonth currentMonth = YearMonth.now();
        List<ExpenseTransaction> monthTransactions = analyticsService.transactionsForMonth(userId, currentMonth);
        BigDecimal monthTotal = analyticsService.total(monthTransactions);
        List<CategorySpend> spending = analyticsService.categorySpending(monthTransactions);
        List<String> facts = new ArrayList<>();

        if (q.contains("most") || q.contains("highest") || q.contains("top category")) {
            if (spending.isEmpty()) return new AssistantResponse("There is no spending recorded this month.", List.of());
            CategorySpend top = spending.get(0);
            facts.add(top.category() + ": ₹" + money(top.amount()));
            facts.add(top.percentage() + "% of this month's spending");
            return new AssistantResponse("You spent the most on " + top.category() + " this month.", facts);
        }

        Optional<String> mentionedCategory = categoryRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(CategoryName::new)
                .filter(item -> q.contains(item.lower()))
                .map(CategoryName::name)
                .findFirst();
        if (mentionedCategory.isPresent()) {
            String name = mentionedCategory.get();
            LocalDateTime start = q.contains("last week") ? LocalDate.now().minusDays(6).atStartOfDay()
                    : currentMonth.atDay(1).atStartOfDay();
            BigDecimal amount = monthTransactions.stream()
                    .filter(tx -> !tx.getOccurredAt().isBefore(start))
                    .filter(tx -> tx.getCategory().getName().equalsIgnoreCase(name))
                    .map(ExpenseTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            facts.add("Period: " + (q.contains("last week") ? "last 7 days" : "current month"));
            facts.add("Transactions: " + monthTransactions.stream()
                    .filter(tx -> !tx.getOccurredAt().isBefore(start))
                    .filter(tx -> tx.getCategory().getName().equalsIgnoreCase(name)).count());
            return new AssistantResponse("You spent ₹" + money(amount) + " on " + name + ".", facts);
        }

        if (q.contains("compare") || q.contains("last month")) {
            BigDecimal previous = analyticsService.total(
                    analyticsService.transactionsForMonth(userId, currentMonth.minusMonths(1)));
            BigDecimal difference = monthTotal.subtract(previous);
            String direction = difference.signum() >= 0 ? "more" : "less";
            facts.add("This month: ₹" + money(monthTotal));
            facts.add("Previous month: ₹" + money(previous));
            return new AssistantResponse("You spent ₹" + money(difference.abs()) + " " + direction + " than the previous month.", facts);
        }

        if (q.contains("can i spend") || q.contains("afford")) {
            BigDecimal requested = extractAmount(q).orElse(BigDecimal.ZERO);
            var dashboard = analyticsService.dashboard(userId);
            BigDecimal remaining = dashboard.remainingBudget();
            boolean canSpend = requested.signum() > 0 && requested.compareTo(remaining) <= 0;
            facts.add("Remaining configured budget: ₹" + money(remaining));
            facts.add("Requested amount: ₹" + money(requested));
            return new AssistantResponse(
                    requested.signum() == 0
                            ? "Mention an amount, for example: Can I spend ₹3,000 this month?"
                            : canSpend
                                ? "Yes, it fits within your remaining configured monthly budget."
                                : "It would exceed your remaining configured monthly budget.",
                    facts);
        }

        facts.add("Spent this month: ₹" + money(monthTotal));
        if (!spending.isEmpty()) facts.add("Highest category: " + spending.get(0).category());
        facts.add("Try asking: Where did I spend the most this month?");
        return new AssistantResponse(
                "Your current monthly spending is ₹" + money(monthTotal) + ". Ask about a category, last month, or whether a purchase fits your budget.",
                facts);
    }

    private Optional<BigDecimal> extractAmount(String question) {
        Matcher matcher = Pattern.compile("(?:₹|rs\\.?|inr)?\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE)
                .matcher(question);
        if (!matcher.find()) return Optional.empty();
        return Optional.of(new BigDecimal(matcher.group(1).replace(",", "")));
    }

    private String money(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private record CategoryName(String name, String lower) {
        CategoryName(com.anmay.spendwise.entity.Category category) {
            this(category.getName(), category.getName().toLowerCase(Locale.ROOT));
        }
    }
}
