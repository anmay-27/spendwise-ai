package com.anmay.spendwise.events;

import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.events.enabled", havingValue = "true")
public class OutboxPublisher {
  private final JdbcTemplate db;
  private final KafkaTemplate<String, String> kafka;

  public OutboxPublisher(JdbcTemplate db, KafkaTemplate<String, String> kafka) {
    this.db = db;
    this.kafka = kafka;
  }

  @Scheduled(fixedDelayString = "${app.events.poll-ms:2000}")
  @Transactional
  public void publish() {
    var rows =
        db.queryForList(
            "select event_id,user_id,event_type,body from event_outbox where published_at is null"
                + " order by created_at,event_id limit 50 for update skip locked");
    for (var row : rows)
      try {
        String type = row.get("event_type").toString();
        String topic =
            (type.startsWith("TRANSACTION_") || type.equals("BUDGET_UPDATED"))
                ? "transaction-events"
                : "notification-events";
        kafka
            .send(topic, row.get("user_id").toString(), row.get("body").toString())
            .get(10, TimeUnit.SECONDS);
        db.update(
            "update event_outbox set published_at=current_timestamp where event_id=?",
            row.get("event_id"));
      } catch (Exception ex) {
        if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
        throw new IllegalStateException("Kafka publish failed; outbox will retry", ex);
      }
  }
}
