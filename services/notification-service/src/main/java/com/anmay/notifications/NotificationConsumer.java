package com.anmay.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationConsumer {
  private final JdbcTemplate db;
  private final ObjectMapper json;

  public NotificationConsumer(JdbcTemplate db, ObjectMapper json) {
    this.db = db;
    this.json = json;
  }

  @KafkaListener(topics = "notification-events")
  @Transactional
  public void consume(String body) throws Exception {
    var event = json.readTree(body);
    if (event.path("schemaVersion").asInt() != 1
        || !event.hasNonNull("eventId")
        || !event.hasNonNull("userId")) throw new IllegalArgumentException("Invalid event");
    db.update(
        "insert into notifications(event_id,user_id,kind,message,body) values(?,?,?,?,?) on"
            + " conflict(event_id) do nothing",
        event.get("eventId").asText(),
        event.get("userId").asLong(),
        event.path("eventType").asText(),
        event.path("payload").path("message").asText(event.path("eventType").asText()),
        body);
  }
}
