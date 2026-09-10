package com.anmay.spendwise.finance;

import com.anmay.spendwise.entity.TransactionType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionRequest(
    @NotBlank @Size(max = 160) String merchant,
    @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
    @NotNull TransactionType type,
    @NotNull Long categoryId,
    @Size(max = 500) String notes,
    @NotNull @PastOrPresent LocalDateTime occurredAt,
    boolean recurring) {}
