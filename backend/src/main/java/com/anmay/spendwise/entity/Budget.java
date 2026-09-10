package com.anmay.spendwise.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
    name = "budgets",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"user_id", "category_id", "budget_month"})
    })
public class Budget {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @ManyToOne(optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal monthlyLimit;

  @Column(nullable = false)
  private int warningPercent;

  @Column(name = "budget_month", nullable = false, length = 7)
  private String month = java.time.YearMonth.now().toString();

  public String getMonth() {
    return month;
  }

  public void setMonth(String month) {
    this.month = month;
  }

  protected Budget() {}

  public Budget(AppUser user, Category category, BigDecimal monthlyLimit, int warningPercent) {
    this.user = user;
    this.category = category;
    this.monthlyLimit = monthlyLimit;
    this.warningPercent = warningPercent;
  }

  public Long getId() {
    return id;
  }

  public AppUser getUser() {
    return user;
  }

  public Category getCategory() {
    return category;
  }

  public BigDecimal getMonthlyLimit() {
    return monthlyLimit;
  }

  public int getWarningPercent() {
    return warningPercent;
  }

  public void setMonthlyLimit(BigDecimal monthlyLimit) {
    this.monthlyLimit = monthlyLimit;
  }

  public void setWarningPercent(int warningPercent) {
    this.warningPercent = warningPercent;
  }
}
