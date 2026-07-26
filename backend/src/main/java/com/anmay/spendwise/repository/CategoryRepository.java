package com.anmay.spendwise.repository;

import com.anmay.spendwise.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserIdOrderByNameAsc(Long userId);
    Optional<Category> findByUserIdAndNameIgnoreCase(Long userId, String name);
}
