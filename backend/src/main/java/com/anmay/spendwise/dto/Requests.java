package com.anmay.spendwise.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class Requests {

    private Requests() {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 80) String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100) String password
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record PaymentPreviewRequest(
            @NotBlank String merchant,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            String description
    ) {
    }

    public record ConfirmPaymentRequest(
            @NotBlank String merchant,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            String description,
            @NotNull Long categoryId
    ) {
    }

    public record CreateCategoryRequest(
            @NotBlank String name,
            String icon
    ) {
    }

    public record SetBudgetRequest(
            @NotNull Long categoryId,
            @NotNull @DecimalMin("1.00") BigDecimal monthlyLimit,
            @Min(1) @Max(100) int warningPercent
    ) {
    }

    public record UpdateCategoryRequest(
            @NotNull Long categoryId
    ) {
    }

    public record AssistantRequest(
            @NotBlank String question
    ) {
    }
}
