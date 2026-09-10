package com.anmay.spendwise.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

public class SessionValidationFilter extends OncePerRequestFilter {
  private final SessionService sessions;

  public SessionValidationFilter(SessionService sessions) {
    this.sessions = sessions;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null
        && auth.getPrincipal() instanceof Jwt jwt
        && !sessions.active(jwt.getClaimAsString("sid"))) {
      SecurityContextHolder.clearContext();
      com.anmay.spendwise.exception.ApiErrors.write(req, res, 401, "Session expired");
      return;
    }
    chain.doFilter(req, res);
  }
}
