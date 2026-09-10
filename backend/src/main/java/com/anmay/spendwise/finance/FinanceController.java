package com.anmay.spendwise.finance;

import com.anmay.spendwise.events.EventOutbox;
import com.anmay.spendwise.security.CurrentUserService;
import java.time.YearMonth;
import java.util.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class FinanceController {
  private final FinanceInsights finance;
  private final CurrentUserService current;
  private final com.anmay.spendwise.service.JsonCache cache;
  private final EventOutbox events;
  private final TransactionTemplate transactions;

  public FinanceController(
      FinanceInsights finance,
      CurrentUserService current,
      com.anmay.spendwise.service.JsonCache cache,
      EventOutbox events,
      org.springframework.transaction.PlatformTransactionManager manager) {
    this.finance = finance;
    this.current = current;
    this.cache = cache;
    this.events = events;
    this.transactions = new TransactionTemplate(manager);
  }

  @GetMapping("/analytics")
  public Object analytics(@RequestParam(required = false) String month) {
    Long uid = current.currentUserId();
    var target = month == null ? YearMonth.now() : YearMonth.parse(month);
    return cache.get("analytics", uid, target.toString(), () -> finance.analytics(uid, target));
  }

  @GetMapping("/subscriptions")
  public Object subscriptions() {
    return finance.subscriptions(current.currentUserId());
  }

  @GetMapping("/insights")
  public Object insights() {
    return finance.analytics(current.currentUserId(), YearMonth.now()).get("insights");
  }

  @PostMapping("/reports/generate")
  public Object report(@RequestParam(required = false) String month) {
    Long uid = current.currentUserId();
    var target = month == null ? YearMonth.now() : YearMonth.parse(month);
    return transactions.execute(
        status -> {
          var report = finance.analytics(uid, target);
          events.append(
              "REPORT_READY",
              null,
              uid,
              Map.of("month", target.toString(), "message", "Your monthly report is ready"));
          return report;
        });
  }
}
