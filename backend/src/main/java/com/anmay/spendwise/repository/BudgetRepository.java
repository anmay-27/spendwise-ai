package com.anmay.spendwise.repository;

import com.anmay.spendwise.entity.Budget;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
  List<Budget> findByUserIdAndMonthOrderByCategoryNameAsc(Long userId, String month);

  Optional<Budget> findByUserIdAndCategoryIdAndMonth(Long userId, Long categoryId, String month);
}
