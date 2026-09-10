package com.anmay.spendwise.events;

import com.anmay.spendwise.finance.FinanceInsights;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.events.enabled", havingValue = "true")
public class SubscriptionReminders {
  private final JdbcTemplate db;
  private final FinanceInsights finance;
  private final EventOutbox events;

  public SubscriptionReminders(JdbcTemplate db, FinanceInsights finance, EventOutbox events) {
    this.db = db;
    this.finance = finance;
    this.events = events;
  }

  @Scheduled(initialDelay = 60000, fixedDelay = 3600000)
  @Transactional
  public void remind() {
    for (Long uid : db.queryForList("select id from app_users", Long.class))
      for (var subscription : finance.subscriptions(uid)) {
        LocalDate next = LocalDate.parse(subscription.get("nextPayment").toString());
        if (next.isBefore(LocalDate.now()) || next.isAfter(LocalDate.now().plusDays(3))) continue;
        String key =
            "reminder:"
                + uid
                + ":"
                + com.anmay.spendwise.security.SessionService.hash(
                    subscription.get("merchant").toString())
                + ":"
                + next;
        if (db.update(
                "insert into detected_signals(signal_key,user_id,kind)"
                    + " values(?,?,'SUBSCRIPTION_REMINDER') on conflict do nothing",
                key,
                uid)
            == 1)
          events.append(
              "SUBSCRIPTION_REMINDER",
              null,
              uid,
              Map.of(
                  "message",
                  subscription.get("merchant") + " payment expected on " + next,
                  "details",
                  subscription));
      }
  }
}
