package com.anmay.spendwise.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expense_transactions")
public class ExpenseTransaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Column(nullable = false)
  private String merchantName;

  @Column(length = 500)
  private String description;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @ManyToOne(optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;

  @Column(nullable = false)
  private String aiSuggestedCategory;

  @Column(nullable = false)
  private double aiConfidence;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentStatus status;

  @Column(nullable = false)
  private LocalDateTime occurredAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionType type = TransactionType.EXPENSE;

  @Column(nullable = false)
  private boolean recurring = false;

  @Column(nullable = false)
  private boolean walletPayment = false;

  @Version private long version;

  public TransactionType getType() {
    return type;
  }

  public boolean isRecurring() {
    return recurring;
  }

  public boolean isWalletPayment() {
    return walletPayment;
  }

  public void markWalletPayment() {
    walletPayment = true;
  }

  public void revise(
      String merchant,
      String notes,
      BigDecimal value,
      Category cat,
      TransactionType kind,
      LocalDateTime date,
      boolean repeat) {
    merchantName = merchant;
    description = notes;
    amount = value;
    category = cat;
    type = kind;
    occurredAt = date;
    recurring = repeat;
  }

  protected ExpenseTransaction() {}

  public ExpenseTransaction(
      AppUser user,
      String merchantName,
      String description,
      BigDecimal amount,
      Category category,
      String aiSuggestedCategory,
      double aiConfidence,
      PaymentStatus status,
      LocalDateTime occurredAt) {
    this.user = user;
    this.merchantName = merchantName;
    this.description = description;
    this.amount = amount;
    this.category = category;
    this.aiSuggestedCategory = aiSuggestedCategory;
    this.aiConfidence = aiConfidence;
    this.status = status;
    this.occurredAt = occurredAt;
  }

  public Long getId() {
    return id;
  }

  public AppUser getUser() {
    return user;
  }

  public String getMerchantName() {
    return merchantName;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public Category getCategory() {
    return category;
  }

  public String getAiSuggestedCategory() {
    return aiSuggestedCategory;
  }

  public double getAiConfidence() {
    return aiConfidence;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public LocalDateTime getOccurredAt() {
    return occurredAt;
  }

  public void setCategory(Category category) {
    this.category = category;
  }
}
