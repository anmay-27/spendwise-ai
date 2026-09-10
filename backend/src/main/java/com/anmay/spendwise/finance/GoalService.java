package com.anmay.spendwise.finance;

import com.anmay.spendwise.events.EventOutbox;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class GoalService {
  private final JdbcTemplate db;
  private final FinanceInsights finance;
  private final EventOutbox events;

  public GoalService(JdbcTemplate db, FinanceInsights finance, EventOutbox events) {
    this.db = db;
    this.finance = finance;
    this.events = events;
  }

  @Transactional(readOnly = true)
  public Object list(Long uid) {
    var rows =
        db.queryForList(
            "select id,name,target_amount,current_amount,target_date,version from goals where"
                + " user_id=? order by target_date,id",
            uid);
    var last = finance.rows(uid, YearMonth.now().minusMonths(1));
    BigDecimal saving = finance.sum(last, "INCOME").subtract(finance.sum(last, "EXPENSE"));
    for (var row : rows) {
      BigDecimal target = (BigDecimal) row.get("target_amount"),
          current = (BigDecimal) row.get("current_amount"),
          remaining = target.subtract(current).max(BigDecimal.ZERO);
      LocalDate deadline = ((java.sql.Date) row.get("target_date")).toLocalDate();
      long months =
          Math.max(
              1,
              java.time.temporal.ChronoUnit.MONTHS.between(
                      YearMonth.now(), YearMonth.from(deadline))
                  + 1);
      row.put(
          "progressPercent", FinanceInsights.percent(current, target).min(BigDecimal.valueOf(100)));
      row.put(
          "requiredMonthlySavings",
          remaining.divide(BigDecimal.valueOf(months), 2, RoundingMode.UP));
      row.put(
          "estimatedCompletion",
          remaining.signum() == 0
              ? LocalDate.now().toString()
              : saving.signum() > 0
                  ? LocalDate.now()
                      .plusMonths(remaining.divide(saving, 0, RoundingMode.UP).longValue())
                      .toString()
                  : null);
      row.put("estimateBasis", "Previous calendar month's net savings");
    }
    return rows;
  }

  public void save(Long uid, Long id, GoalController.GoalRequest r) {
    if (id == null)
      db.update(
          "insert into goals(user_id,name,target_amount,current_amount,target_date)"
              + " values(?,?,?,?,?)",
          uid,
          r.name().trim(),
          r.targetAmount(),
          r.currentAmount(),
          java.sql.Date.valueOf(r.targetDate()));
    else {
      var old =
          db.queryForList(
              "select current_amount,target_amount from goals where id=? and user_id=? for update",
              id,
              uid);
      if (old.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found");
      if (db.update(
              "update goals set"
                  + " name=?,target_amount=?,current_amount=?,target_date=?,version=version+1 where"
                  + " id=? and user_id=? and version=?",
              r.name().trim(),
              r.targetAmount(),
              r.currentAmount(),
              java.sql.Date.valueOf(r.targetDate()),
              id,
              uid,
              r.version())
          == 0)
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Goal changed; refresh and retry");
      BigDecimal before =
          FinanceInsights.percent(
              (BigDecimal) old.get(0).get("current_amount"),
              (BigDecimal) old.get(0).get("target_amount"));
      BigDecimal after = FinanceInsights.percent(r.currentAmount(), r.targetAmount());
      for (int level : new int[] {25, 50, 75, 100})
        if (before.doubleValue() < level && after.doubleValue() >= level)
          events.append(
              "GOAL_MILESTONE_REACHED",
              null,
              uid,
              Map.of(
                  "message",
                  r.name() + " reached " + level + "%",
                  "goalId",
                  id,
                  "threshold",
                  level));
    }
  }

  public void delete(Long uid, Long id) {
    if (db.update("delete from goals where id=? and user_id=?", id, uid) == 0)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found");
  }
}
