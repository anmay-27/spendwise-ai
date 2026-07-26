package com.anmay.spendwise.repository;

import com.anmay.spendwise.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    Optional<AppUser> findByGoogleSubject(String googleSubject);
    boolean existsByEmailIgnoreCase(String email);
}
