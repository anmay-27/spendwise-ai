package com.anmay.spendwise.repository;

import com.anmay.spendwise.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {}
