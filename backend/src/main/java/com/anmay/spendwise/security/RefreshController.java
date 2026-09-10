package com.anmay.spendwise.security;

import jakarta.servlet.http.*;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class RefreshController {
  private final SessionService sessions;
  private final AuthCookieService cookies;

  public RefreshController(SessionService sessions, AuthCookieService cookies) {
    this.sessions = sessions;
    this.cookies = cookies;
  }

  @PostMapping("/refresh")
  public Map<String, Boolean> refresh(
      @CookieValue(name = "spendwise_refresh") String refresh, HttpServletResponse response) {
    var tokens = sessions.rotate(refresh);
    cookies.writeToken(response, tokens.access());
    cookies.writeRefresh(response, tokens.refresh());
    return Map.of("refreshed", true);
  }
}
