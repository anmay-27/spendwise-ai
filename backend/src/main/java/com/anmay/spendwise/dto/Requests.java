package com.anmay.spendwise.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class Requests {
  private Requests() {}

  public record RegisterRequest(
      @NotBlank @Size(max = 100) String name,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 8, max = 72) String password) {}

  public record LoginRequest(
      @NotBlank @Email @Size(max = 254) String email, @NotBlank String password) {}

  public record PaymentRequest(
      Long userId,
      @NotBlank @Size(max = 160) String merchant,
      @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
      @Size(max = 500) String description) {}

  public record CreateCategoryRequest(
      Long userId, @NotBlank @Size(max = 80) String name, @Size(max = 32) String icon) {}

  public record SetBudgetRequest(
      Long userId,
      @NotNull Long categoryId,
      @NotNull @DecimalMin("1.00") @Digits(integer = 12, fraction = 2) BigDecimal monthlyLimit,
      @Min(1) @Max(100) int warningPercent,
      @Pattern(regexp = "\\d{4}-\\d{2}") String month) {}

  public record UpdateCategoryRequest(@NotNull Long categoryId) {}

  public record AssistantRequest(Long userId, @NotBlank @Size(max = 1000) String question) {}
}
