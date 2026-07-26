package com.anmay.spendwise.repository;

import com.anmay.spendwise.entity.ExpenseTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseTransactionRepository extends JpaRepository<ExpenseTransaction, Long> {
    List<ExpenseTransaction> findByUserIdOrderByOccurredAtDesc(Long userId);
    List<ExpenseTransaction> findByUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(
            Long userId, LocalDateTime start, LocalDateTime end);
}
