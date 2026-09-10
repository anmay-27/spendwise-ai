package com.anmay.spendwise.controller;

import com.anmay.spendwise.dto.Requests.LoginRequest;
import com.anmay.spendwise.dto.Requests.RegisterRequest;
import com.anmay.spendwise.dto.Responses.AuthUserResponse;
import com.anmay.spendwise.dto.Responses.CsrfResponse;
import com.anmay.spendwise.security.AuthCookieService;
import com.anmay.spendwise.security.CurrentUserService;
import com.anmay.spendwise.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  @org.springframework.beans.factory.annotation.Autowired
  private com.anmay.spendwise.security.SessionService sessions;

  private final AuthService authService;
  private final AuthCookieService cookieService;
  private final CurrentUserService currentUserService;

  public AuthController(
      AuthService authService,
      AuthCookieService cookieService,
      CurrentUserService currentUserService) {
    this.authService = authService;
    this.cookieService = cookieService;
    this.currentUserService = currentUserService;
  }

  @GetMapping("/csrf")
  public CsrfResponse csrf(CsrfToken token) {
    return new CsrfResponse(token.getToken(), token.getHeaderName());
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public AuthUserResponse register(
      @Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
    AuthService.AuthResult result = authService.register(request);
    cookieService.writeToken(response, result.token());
    cookieService.writeRefresh(response, result.refresh());
    return result.user();
  }

  @PostMapping("/login")
  public AuthUserResponse login(
      @Valid @RequestBody LoginRequest request, HttpServletResponse response) {
    AuthService.AuthResult result = authService.login(request);
    cookieService.writeToken(response, result.token());
    cookieService.writeRefresh(response, result.refresh());
    return result.user();
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(HttpServletRequest request, HttpServletResponse response) {
    var auth =
        org.springframework.security.core.context.SecurityContextHolder.getContext()
            .getAuthentication();
    String sid =
        auth != null
                && auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt
            ? jwt.getClaimAsString("sid")
            : null;
    String refresh =
        request.getCookies() == null
            ? null
            : java.util.Arrays.stream(request.getCookies())
                .filter(c -> c.getName().equals("spendwise_refresh"))
                .map(jakarta.servlet.http.Cookie::getValue)
                .findFirst()
                .orElse(null);
    sessions.revoke(sid, refresh);
    cookieService.clearToken(response);
    if (request.getSession(false) != null) {
      request.getSession(false).invalidate();
    }
  }

  @GetMapping("/me")
  public AuthUserResponse me() {
    return authService.view(currentUserService.currentUser());
  }
}
