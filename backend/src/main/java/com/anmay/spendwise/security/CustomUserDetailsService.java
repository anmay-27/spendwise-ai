package com.anmay.spendwise.security;

import com.anmay.spendwise.entity.AppUser;
import com.anmay.spendwise.repository.AppUserRepository;
import java.util.UUID;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
  private final AppUserRepository userRepository;
  private final String impossibleOauthPassword;

  public CustomUserDetailsService(
      AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.impossibleOauthPassword = passwordEncoder.encode(UUID.randomUUID().toString());
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    AppUser user =
        userRepository
            .findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UsernameNotFoundException("Account not found"));

    String password =
        user.getPasswordHash() == null ? impossibleOauthPassword : user.getPasswordHash();
    return User.withUsername(user.getEmail())
        .password(password)
        .roles(user.getRole().name())
        .build();
  }
}
