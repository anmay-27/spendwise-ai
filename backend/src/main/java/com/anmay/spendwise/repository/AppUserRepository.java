package com.anmay.spendwise.repository;

import com.anmay.spendwise.entity.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

  Optional<AppUser> findByEmailIgnoreCase(String email);

  Optional<AppUser> findByGoogleSubject(String googleSubject);

  boolean existsByEmailIgnoreCase(String email);
}
