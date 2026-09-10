package com.anmay.spendwise.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "categories",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "name"})})
public class Category {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String icon;

  @Column(nullable = false)
  private boolean systemDefined;

  protected Category() {}

  public Category(AppUser user, String name, String icon, boolean systemDefined) {
    this.user = user;
    this.name = name;
    this.icon = icon;
    this.systemDefined = systemDefined;
  }

  public Long getId() {
    return id;
  }

  public AppUser getUser() {
    return user;
  }

  public String getName() {
    return name;
  }

  public String getIcon() {
    return icon;
  }

  public boolean isSystemDefined() {
    return systemDefined;
  }
}
