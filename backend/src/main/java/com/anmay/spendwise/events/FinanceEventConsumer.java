package com.anmay.spendwise.events;

import com.anmay.spendwise.finance.FinanceInsights;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FinanceEventConsumer {
  @org.springframework.beans.factory.annotation.Autowired
  private com.anmay.spendwise.service.MlServiceClient ml;

  private final JdbcTemplate db;
  private final ObjectMapper json;
  private final FinanceInsights finance;
  private final EventOutbox events;

  public FinanceEventConsumer(
      JdbcTemplate db, ObjectMapper json, FinanceInsights finance, EventOutbox events) {
    this.db = db;
    this.json = json;
    this.finance = finance;
    this.events = events;
  }

  @KafkaListener(topics = "transaction-events")
  @Transactional
  public void consume(String body) throws Exception {
    DomainEvent event = json.readValue(body, DomainEvent.class);
    if (event.schemaVersion() != 1 || event.userId() == null || event.eventId() == null)
      throw new IllegalArgumentException("Unsupported event");
    if (db.update(
            "insert into processed_events(consumer,event_id) values('finance-v1',?) on conflict do"
                + " nothing",
            event.eventId().toString())
        == 0) return;
    Long uid = event.userId();
    if (Boolean.TRUE.equals(event.payload().get("categoryCorrection"))) {
      var corrections =
          db.queryForList(
              "select t.merchant_name,t.description,c.name from expense_transactions t join"
                  + " categories c on c.id=t.category_id where t.id=? and t.user_id=?",
              event.transactionId(),
              uid);
      if (!corrections.isEmpty()) {
        var correction = corrections.get(0);
        ml.sendFeedback(
            uid,
            correction.get("merchant_name").toString(),
            java.util.Objects.toString(correction.get("description"), ""),
            correction.get("name").toString());
      }
    }
    // Recompute from committed source data: duplicates and late update/delete events cannot add
    // spending twice.
    var budgets =
        db.queryForList(
            "select b.id,b.category_id,b.budget_month,b.monthly_limit,c.name from budgets b join"
                + " categories c on c.id=b.category_id where b.user_id=?",
            uid);
    for (var b : budgets) {
      var month = YearMonth.parse(b.get("budget_month").toString());
      BigDecimal spent =
          finance.rows(uid, month).stream()
              .filter(
                  r ->
                      r.get("type").equals("EXPENSE")
                          && r.get("category_id").equals(b.get("category_id")))
              .map(r -> (BigDecimal) r.get("amount"))
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      BigDecimal percentage = FinanceInsights.percent(spent, (BigDecimal) b.get("monthly_limit"));
      for (int level : new int[] {80, 90, 100})
        if (percentage.doubleValue() >= level)
          signal(
              "budget:" + b.get("id") + ":" + level,
              "BUDGET_THRESHOLD_REACHED",
              uid,
              null,
              Map.of(
                  "message",
                  b.get("name") + " budget reached " + level + "% for " + month,
                  "category",
                  b.get("name"),
                  "spent",
                  spent,
                  "budget",
                  b.get("monthly_limit"),
                  "percentage",
                  percentage));
    }
    for (var anomaly : finance.anomalies(uid, YearMonth.now()))
      signal(
          "anomaly:" + anomaly.get("transactionId"),
          "SPENDING_ANOMALY_DETECTED",
          uid,
          ((Number) anomaly.get("transactionId")).longValue(),
          Map.of("message", "Unusual spending at " + anomaly.get("merchant"), "details", anomaly));
    for (var subscription : finance.subscriptions(uid))
      signal(
          "subscription:" + uid + ":" + subscription.get("merchant"),
          "SUBSCRIPTION_DETECTED",
          uid,
          null,
          Map.of(
              "message",
              "Recurring subscription detected: " + subscription.get("merchant"),
              "details",
              subscription));
  }

  private void signal(String key, String type, Long uid, Long tx, Map<String, Object> payload) {
    if (db.update(
            "insert into detected_signals(signal_key,user_id,kind) values(?,?,?) on conflict do"
                + " nothing",
            key,
            uid,
            type)
        == 1) events.append(type, tx, uid, payload);
  }
}
