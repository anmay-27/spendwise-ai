package com.anmay.spendwise.security;

import com.anmay.spendwise.entity.AppUser;
import com.anmay.spendwise.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
  private final AppUserRepository userRepository;

  public CurrentUserService(AppUserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public AppUser currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new IllegalArgumentException("You must be logged in");
    }

    String email;
    Object principal = authentication.getPrincipal();
    if (principal instanceof Jwt jwt) {
      email = jwt.getSubject();
    } else if (principal instanceof OAuth2User oauth2User) {
      email = oauth2User.getAttribute("email");
    } else {
      email = authentication.getName();
    }

    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("Authenticated account has no email address");
    }

    return userRepository
        .findByEmailIgnoreCase(email)
        .orElseThrow(() -> new IllegalArgumentException("Authenticated user was not found"));
  }

  public Long currentUserId() {
    return currentUser().getId();
  }
}
