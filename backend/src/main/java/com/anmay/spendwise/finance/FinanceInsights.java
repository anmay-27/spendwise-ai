package com.anmay.spendwise.finance;

import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FinanceInsights {
  private final JdbcTemplate db;

  public FinanceInsights(JdbcTemplate db) {
    this.db = db;
  }

  public List<Map<String, Object>> rows(Long uid, YearMonth month) {
    return db.queryForList(
        "select t.id,t.amount,t.type,t.merchant_name,t.occurred_at,t.category_id,c.name as category"
            + " from expense_transactions t join categories c on c.id=t.category_id where"
            + " t.user_id=? and t.occurred_at>=? and t.occurred_at<? and t.status='SUCCESSFUL'"
            + " order by t.occurred_at",
        uid,
        java.sql.Timestamp.valueOf(month.atDay(1).atStartOfDay()),
        java.sql.Timestamp.valueOf(month.plusMonths(1).atDay(1).atStartOfDay()));
  }

  public Map<String, Object> analytics(Long uid, YearMonth month) {
    var rows = rows(uid, month);
    var previous = rows(uid, month.minusMonths(1));
    BigDecimal income = sum(rows, "INCOME"),
        expense = sum(rows, "EXPENSE"),
        last = sum(previous, "EXPENSE"),
        savings = income.subtract(expense);
    int days =
        month.equals(YearMonth.now()) ? LocalDate.now().getDayOfMonth() : month.lengthOfMonth();
    var categories = group(rows, "category");
    var prior = group(previous, "category");
    var insights = new ArrayList<String>();
    categories.forEach(
        (category, amount) -> {
          BigDecimal old = prior.getOrDefault(category, BigDecimal.ZERO);
          if (old.signum() > 0) {
            BigDecimal change = percent(amount.subtract(old), old);
            insights.add(
                category
                    + " spending "
                    + (change.signum() >= 0 ? "increased " : "decreased ")
                    + change.abs()
                    + "% compared with last month.");
          }
        });
    var subscriptions = subscriptions(uid);
    BigDecimal subscriptionTotal =
        subscriptions.stream()
            .map(x -> (BigDecimal) x.get("monthlyCost"))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    insights.add(
        "Detected recurring subscriptions cost approximately ₹" + subscriptionTotal + "/month.");
    BigDecimal delivery =
        rows.stream()
            .filter(
                r ->
                    r.get("type").equals("EXPENSE")
                        && r.get("merchant_name")
                            .toString()
                            .toLowerCase(Locale.ROOT)
                            .matches(".*(swiggy|zomato).*"))
            .map(r -> (BigDecimal) r.get("amount"))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    insights.add(
        "Reducing this month's food delivery by 20% would save approximately ₹"
            + delivery.multiply(new BigDecimal("2.4")).setScale(2, RoundingMode.HALF_UP)
            + " annually if this month is representative.");
    var daily = new TreeMap<String, BigDecimal>();
    var weekly = new TreeMap<String, BigDecimal>();
    for (var r : rows)
      if (r.get("type").equals("EXPENSE")) {
        LocalDate d = ((java.sql.Timestamp) r.get("occurred_at")).toLocalDateTime().toLocalDate();
        daily.merge(d.toString(), (BigDecimal) r.get("amount"), BigDecimal::add);
        weekly.merge(
            d.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toString(),
            (BigDecimal) r.get("amount"),
            BigDecimal::add);
      }
    var result = new LinkedHashMap<String, Object>();
    var trend = new LinkedHashMap<String, BigDecimal>();
    for (int i = 5; i >= 0; i--)
      trend.put(month.minusMonths(i).toString(), sum(rows(uid, month.minusMonths(i)), "EXPENSE"));
    result.put("trend", trend);
    result.put("month", month.toString());
    result.put("income", income);
    result.put("expenses", expense);
    result.put("savings", savings);
    result.put("savingsRate", percent(savings, income));
    result.put("previousExpenses", last);
    result.put("changePercent", last.signum() == 0 ? null : percent(expense.subtract(last), last));
    result.put("categories", categories);
    result.put("merchants", group(rows, "merchant_name"));
    result.put("daily", daily);
    result.put("weekly", weekly);
    result.put(
        "largestTransaction",
        rows.stream()
            .map(r -> (BigDecimal) r.get("amount"))
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO));
    result.put(
        "averageDailySpend", expense.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP));
    result.put("subscriptionTotal", subscriptionTotal);
    result.put("subscriptions", subscriptions);
    result.put("insights", insights);
    result.put("anomalies", anomalies(uid, month));
    return result;
  }

  public List<Map<String, Object>> subscriptions(Long uid) {
    var rows =
        db.queryForList(
            "select merchant_name,amount,occurred_at from expense_transactions where user_id=? and"
                + " type='EXPENSE' and status='SUCCESSFUL' and occurred_at>=? order by occurred_at",
            uid,
            java.sql.Timestamp.valueOf(LocalDate.now().minusMonths(12).atStartOfDay()));
    var groups = new LinkedHashMap<String, List<Map<String, Object>>>();
    for (var r : rows)
      groups
          .computeIfAbsent(
              r.get("merchant_name").toString().trim().toLowerCase(Locale.ROOT),
              k -> new ArrayList<>())
          .add(r);
    var result = new ArrayList<Map<String, Object>>();
    groups.forEach(
        (merchant, items) -> {
          if (items.size() < 3) return;
          var recent = items.subList(items.size() - 3, items.size());
          BigDecimal amount = (BigDecimal) recent.get(2).get("amount");
          if (amount.signum() <= 0) return;
          for (int i = 0; i < 2; i++) {
            BigDecimal other = (BigDecimal) recent.get(i).get("amount");
            if (other.subtract(amount).abs().divide(amount, 4, RoundingMode.HALF_UP).doubleValue()
                > .05) return;
            long days =
                java.time.temporal.ChronoUnit.DAYS.between(
                    date(recent.get(i)), date(recent.get(i + 1)));
            if (days < 25 || days > 35) return;
          }
          LocalDate last = date(recent.get(2));
          if (last.isBefore(LocalDate.now().minusDays(45))) return;
          result.add(
              Map.of(
                  "merchant",
                  merchant,
                  "monthlyCost",
                  amount,
                  "annualCost",
                  amount.multiply(BigDecimal.valueOf(12)),
                  "frequency",
                  "MONTHLY",
                  "nextPayment",
                  last.plusMonths(1).toString(),
                  "confidence",
                  0.85));
        });
    return result;
  }

  private LocalDate date(Map<String, Object> row) {
    return ((java.sql.Timestamp) row.get("occurred_at")).toLocalDateTime().toLocalDate();
  }

  public List<Map<String, Object>> anomalies(Long uid, YearMonth month) {
    var result = new ArrayList<Map<String, Object>>();
    for (var row : rows(uid, month)) {
      if (!row.get("type").equals("EXPENSE")) continue;
      var history =
          db.queryForList(
              "select amount from expense_transactions where user_id=? and category_id=? and"
                  + " type='EXPENSE' and occurred_at<? and occurred_at>=? order by occurred_at desc"
                  + " limit 60",
              uid,
              row.get("category_id"),
              row.get("occurred_at"),
              java.sql.Timestamp.valueOf(
                  ((java.sql.Timestamp) row.get("occurred_at")).toLocalDateTime().minusMonths(6)));
      if (history.size() < 5) continue;
      double mean =
          history.stream()
              .mapToDouble(r -> ((BigDecimal) r.get("amount")).doubleValue())
              .average()
              .orElse(0);
      double variance =
          history.stream()
                  .mapToDouble(
                      r -> Math.pow(((BigDecimal) r.get("amount")).doubleValue() - mean, 2))
                  .sum()
              / history.size();
      double sd = Math.sqrt(variance), amount = ((BigDecimal) row.get("amount")).doubleValue();
      if (amount > mean * 2 && amount > mean + 3 * sd)
        result.add(
            Map.of(
                "transactionId",
                row.get("id"),
                "merchant",
                row.get("merchant_name"),
                "amount",
                row.get("amount"),
                "historicalAverage",
                BigDecimal.valueOf(mean).setScale(2, RoundingMode.HALF_UP),
                "reason",
                "Above twice the category average and three standard deviations",
                "sampleSize",
                history.size()));
    }
    return result;
  }

  public Map<String, BigDecimal> group(List<Map<String, Object>> rows, String key) {
    var result = new TreeMap<String, BigDecimal>();
    for (var r : rows)
      if (r.get("type").equals("EXPENSE"))
        result.merge(r.get(key).toString(), (BigDecimal) r.get("amount"), BigDecimal::add);
    return result;
  }

  public BigDecimal sum(List<Map<String, Object>> rows, String type) {
    return rows.stream()
        .filter(r -> r.get("type").equals(type))
        .map(r -> (BigDecimal) r.get("amount"))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public static BigDecimal percent(BigDecimal n, BigDecimal d) {
    return d.signum() == 0
        ? BigDecimal.ZERO
        : n.multiply(BigDecimal.valueOf(100)).divide(d, 2, RoundingMode.HALF_UP);
  }
}
