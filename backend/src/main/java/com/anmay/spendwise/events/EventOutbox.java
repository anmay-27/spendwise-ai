package com.anmay.spendwise.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class EventOutbox {
  private final JdbcTemplate db;
  private final ObjectMapper json;

  public EventOutbox(JdbcTemplate db, ObjectMapper json) {
    this.db = db;
    this.json = json;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void append(String type, Long transactionId, Long userId, Map<String, Object> payload) {
    try {
      var event =
          new DomainEvent(
              UUID.randomUUID(), type, transactionId, userId, Instant.now(), 1, payload);
      db.update(
          "insert into event_outbox(event_id,user_id,event_type,body,created_at) values(?,?,?,?,?)",
          event.eventId().toString(),
          userId,
          type,
          json.writeValueAsString(event),
          java.sql.Timestamp.from(event.timestamp()));
      db.update("update app_users set data_version=data_version+1 where id=?", userId);
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      throw new IllegalStateException("Cannot serialize event", ex);
    }
  }
}
