package com.anmay.spendwise.repository;

import com.anmay.spendwise.entity.ExpenseTransaction;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseTransactionRepository
    extends JpaRepository<ExpenseTransaction, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<ExpenseTransaction> {
  java.util.Optional<ExpenseTransaction> findByIdAndUserId(Long id, Long userId);

  List<ExpenseTransaction> findByUserIdOrderByOccurredAtDesc(Long userId);

  List<ExpenseTransaction>
      findByUserIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
          Long userId, LocalDateTime start, LocalDateTime end);
}
