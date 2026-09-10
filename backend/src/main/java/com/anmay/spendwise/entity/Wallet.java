package com.anmay.spendwise.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
public class Wallet {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private AppUser user;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal balance;

  protected Wallet() {}

  public Wallet(AppUser user, BigDecimal balance) {
    this.user = user;
    this.balance = balance;
  }

  public Long getId() {
    return id;
  }

  public AppUser getUser() {
    return user;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public void setBalance(BigDecimal balance) {
    this.balance = balance;
  }
}
