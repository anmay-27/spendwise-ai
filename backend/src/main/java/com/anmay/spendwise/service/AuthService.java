package com.anmay.spendwise.service;

import com.anmay.spendwise.dto.Requests.LoginRequest;
import com.anmay.spendwise.dto.Requests.RegisterRequest;
import com.anmay.spendwise.dto.Responses.AuthUserResponse;
import com.anmay.spendwise.entity.AppUser;
import com.anmay.spendwise.repository.AppUserRepository;
import com.anmay.spendwise.security.JwtService;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  @org.springframework.beans.factory.annotation.Autowired
  private com.anmay.spendwise.security.SessionService sessions;

  private final AppUserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final AccountProvisioningService provisioningService;
  private final JwtService jwtService;

  public AuthService(
      AppUserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AuthenticationManager authenticationManager,
      AccountProvisioningService provisioningService,
      JwtService jwtService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.provisioningService = provisioningService;
    this.jwtService = jwtService;
  }

  @Transactional
  public AuthResult register(RegisterRequest request) {
    String email = normalizeEmail(request.email());
    if (userRepository.existsByEmailIgnoreCase(email)) {
      throw new IllegalArgumentException("An account with this email already exists");
    }

    AppUser user =
        userRepository.save(
            new AppUser(request.name().trim(), email, passwordEncoder.encode(request.password())));
    provisioningService.provision(user);
    return result(user);
  }

  @Transactional
  public AuthResult login(LoginRequest request) {
    String email = normalizeEmail(request.email());
    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(email, request.password()));
    } catch (AuthenticationException exception) {
      throw new org.springframework.security.authentication.BadCredentialsException(
          "Invalid email or password");
    }

    AppUser user =
        userRepository
            .findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
    return result(user);
  }

  public AuthUserResponse view(AppUser user) {
    return new AuthUserResponse(
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getRole().name(),
        user.getGoogleSubject() != null);
  }

  private AuthResult result(AppUser user) {
    var tokens = sessions.create(user);
    return new AuthResult(view(user), tokens.access(), tokens.refresh());
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  public record AuthResult(AuthUserResponse user, String token, String refresh) {}
}
