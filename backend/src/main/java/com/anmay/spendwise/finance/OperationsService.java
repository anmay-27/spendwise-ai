package com.anmay.spendwise.finance;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class OperationsService {
  private final JdbcTemplate database;

  public OperationsService(JdbcTemplate database) {
    this.database = database;
  }

  public Map<String, Object> overview() {
    return Map.of(
        "users", database.queryForObject("select count(*) from app_users", Long.class),
        "pendingEvents",
            database.queryForObject(
                "select count(*) from event_outbox where published_at is null", Long.class),
        "processedEvents",
            database.queryForObject("select count(*) from processed_events", Long.class));
  }
}
