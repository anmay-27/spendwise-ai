package com.anmay.spendwise.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "app_users")
public class AppUser {

  public enum Role {
    USER,
    ADMIN
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash")
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role = Role.USER;

  @Column(name = "google_subject", unique = true)
  private String googleSubject;

  protected AppUser() {}

  public AppUser(String name, String email, String passwordHash) {
    this.name = name;
    this.email = email;
    this.passwordHash = passwordHash;
    this.role = Role.USER;
  }

  public static AppUser googleUser(String name, String email, String googleSubject) {
    AppUser user = new AppUser();
    user.name = name;
    user.email = email;
    user.googleSubject = googleSubject;
    user.role = Role.USER;
    return user;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public Role getRole() {
    return role;
  }

  public String getGoogleSubject() {
    return googleSubject;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setGoogleSubject(String googleSubject) {
    this.googleSubject = googleSubject;
  }
}
