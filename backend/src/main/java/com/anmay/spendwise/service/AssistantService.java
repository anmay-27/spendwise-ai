package com.anmay.spendwise.service;

import com.anmay.spendwise.dto.Responses.AssistantResponse;
import com.anmay.spendwise.finance.FinanceInsights;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;
import org.springframework.stereotype.Service;

@Service
public class AssistantService {
  private final FinanceInsights finance;

  public AssistantService(FinanceInsights finance) {
    this.finance = finance;
  }

  public AssistantResponse answer(Long uid, String question) {
    String q = question.toLowerCase(Locale.ROOT);
    YearMonth month = q.contains("last month") ? YearMonth.now().minusMonths(1) : YearMonth.now();
    var rows = finance.rows(uid, month);
    var categories = finance.group(rows, "category");
    if (q.contains("subscription")) {
      var subs = finance.subscriptions(uid);
      BigDecimal amount =
          subs.stream()
              .map(s -> (BigDecimal) s.get("monthlyCost"))
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      return answer(
          "Detected subscriptions total ₹" + amount + " per month.",
          List.of(
              "Annual estimate: ₹" + amount.multiply(BigDecimal.valueOf(12)),
              "Requires at least three monthly payments"));
    }
    if (q.contains("save") && (q.contains("month") || q.contains("lakh"))) {
      Matcher amountMatcher =
          Pattern.compile("(?:₹|rs\\.?|inr)?\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*(lakh|k)?")
              .matcher(q);
      if (!amountMatcher.find())
        return answer("Specify a target amount and number of months.", List.of());
      BigDecimal target = new BigDecimal(amountMatcher.group(1).replace(",", ""));
      if ("lakh".equals(amountMatcher.group(2)))
        target = target.multiply(BigDecimal.valueOf(100000));
      if ("k".equals(amountMatcher.group(2))) target = target.multiply(BigDecimal.valueOf(1000));
      Matcher monthsMatcher = Pattern.compile("(\\d+)\\s*months?").matcher(q);
      int months =
          monthsMatcher.find()
              ? Integer.parseInt(monthsMatcher.group(1))
              : q.contains("six") ? 6 : 0;
      if (months < 1 || months > 600)
        return answer("Specify a horizon from 1 to 600 months.", List.of());
      var previous = finance.rows(uid, YearMonth.now().minusMonths(1));
      BigDecimal
          saving = finance.sum(previous, "INCOME").subtract(finance.sum(previous, "EXPENSE")),
          required = target.divide(BigDecimal.valueOf(months), 2, RoundingMode.UP);
      return answer(
          "You need ₹" + required + " per month to save ₹" + target + " in " + months + " months.",
          List.of(
              "Previous month's net savings: ₹" + saving,
              saving.compareTo(required) >= 0
                  ? "That fits your previous month's pace; it is an estimate."
                  : "That exceeds your previous month's pace."));
    }
    if (q.contains("increased") || q.contains("increase")) {
      var now = finance.group(finance.rows(uid, YearMonth.now()), "category");
      var old = finance.group(finance.rows(uid, YearMonth.now().minusMonths(1)), "category");
      var highest =
          now.entrySet().stream()
              .max(
                  Comparator.comparing(
                      e -> e.getValue().subtract(old.getOrDefault(e.getKey(), BigDecimal.ZERO))));
      if (highest.isEmpty()) return answer("No spending recorded.", List.of());
      var top = highest.get();
      BigDecimal difference =
          top.getValue().subtract(old.getOrDefault(top.getKey(), BigDecimal.ZERO));
      return answer(
          difference.signum() > 0
              ? top.getKey() + " increased the most by amount: ₹" + difference + "."
              : "No category spending increased.",
          List.of("Comparison: current and previous calendar month"));
    }
    if (q.contains("compare")) {
      var now = finance.sum(finance.rows(uid, YearMonth.now()), "EXPENSE");
      var old = finance.sum(finance.rows(uid, YearMonth.now().minusMonths(1)), "EXPENSE");
      return answer(
          "This month: ₹" + now + "; last month: ₹" + old + ".",
          List.of("Difference: ₹" + now.subtract(old)));
    }
    for (String category : categories.keySet())
      if (q.contains(category.toLowerCase(Locale.ROOT))) {
        BigDecimal amount = categories.get(category);
        if (q.contains("last week")) {
          var combined =
              new ArrayList<Map<String, Object>>(finance.rows(uid, YearMonth.now().minusMonths(1)));
          combined.addAll(finance.rows(uid, YearMonth.now()));
          LocalDateTime start = LocalDate.now().minusDays(6).atStartOfDay();
          amount =
              combined.stream()
                  .filter(
                      r ->
                          r.get("type").equals("EXPENSE")
                              && r.get("category").equals(category)
                              && !((java.sql.Timestamp) r.get("occurred_at"))
                                  .toLocalDateTime()
                                  .isBefore(start))
                  .map(r -> (BigDecimal) r.get("amount"))
                  .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return answer(
            "You spent ₹" + amount + " on " + category + ".",
            List.of("Period: " + (q.contains("last week") ? "last seven days" : month)));
      }
    if (q.contains("reduce"))
      return answer(
          "Start with your largest discretionary categories and recurring charges.",
          (List<String>) finance.analytics(uid, month).get("insights"));
    var highest = categories.entrySet().stream().max(Map.Entry.comparingByValue());
    if (q.contains("most") || q.contains("highest"))
      return answer(
          highest
              .map(e -> "Your largest category was " + e.getKey() + " at ₹" + e.getValue() + ".")
              .orElse("No spending recorded."),
          List.of("Period: " + month));
    return answer(
        "Total expenses for " + month + ": ₹" + finance.sum(rows, "EXPENSE") + ".",
        List.of(
            "Ask about a category, subscriptions, month comparison, savings targets, or reducing"
                + " expenses."));
  }

  private AssistantResponse answer(String text, List<String> facts) {
    return new AssistantResponse(text, facts);
  }
}
