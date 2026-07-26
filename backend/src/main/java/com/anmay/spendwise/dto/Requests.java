package com.anmay.spendwise.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class Requests {
    private Requests() {}

    public record PaymentRequest(
            @NotNull Long userId,
            @NotBlank String merchant,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            String description) {}

    public record CreateCategoryRequest(
            @NotNull Long userId,
            @NotBlank String name,
            String icon) {}

    public record SetBudgetRequest(
            @NotNull Long userId,
            @NotNull Long categoryId,
            @NotNull @DecimalMin("1.00") BigDecimal monthlyLimit,
            @Min(1) @Max(100) int warningPercent) {}

    public record UpdateCategoryRequest(
            @NotNull Long categoryId) {}

    public record AssistantRequest(
            @NotNull Long userId,
            @NotBlank String question) {}
}
