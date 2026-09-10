package com.anmay.spendwise.repository;

import com.anmay.spendwise.entity.Wallet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select w from Wallet w where w.user.id = :userId")
  Optional<Wallet> lockByUserId(
      @org.springframework.data.repository.query.Param("userId") Long userId);

  Optional<Wallet> findByUserId(Long userId);
}
