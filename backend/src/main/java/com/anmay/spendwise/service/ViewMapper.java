package com.anmay.spendwise.service;

import com.anmay.spendwise.dto.Responses.TransactionView;
import com.anmay.spendwise.entity.ExpenseTransaction;

public final class ViewMapper {
    private ViewMapper() {}

    public static TransactionView transaction(ExpenseTransaction tx) {
        return new TransactionView(
                tx.getId(),
                tx.getMerchantName(),
                tx.getDescription(),
                tx.getAmount(),
                tx.getCategory().getId(),
                tx.getCategory().getName(),
                tx.getCategory().getIcon(),
                tx.getAiSuggestedCategory(),
                tx.getAiConfidence(),
                tx.getStatus().name(),
                tx.getOccurredAt()
        );
    }
}
