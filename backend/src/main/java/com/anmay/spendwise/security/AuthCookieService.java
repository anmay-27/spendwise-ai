package com.anmay.spendwise.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class AuthCookieService {
  private final JwtService jwtService;
  private final boolean secure;
  private final String sameSite;

  public AuthCookieService(
      JwtService jwtService,
      @Value("${app.security.cookie-secure:false}") boolean secure,
      @Value("${app.security.cookie-same-site:Lax}") String sameSite) {
    this.jwtService = jwtService;
    this.secure = secure;
    this.sameSite = sameSite;
  }

  public void writeToken(HttpServletResponse response, String token) {
    ResponseCookie cookie =
        ResponseCookie.from(CookieBearerTokenResolver.COOKIE_NAME, token)
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/")
            .maxAge(jwtService.lifetime())
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  public void writeRefresh(HttpServletResponse response, String token) {
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        ResponseCookie.from("spendwise_refresh", token)
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/api/auth")
            .maxAge(java.time.Duration.ofDays(14))
            .build()
            .toString());
  }

  public void clearToken(HttpServletResponse response) {
    ResponseCookie cookie =
        ResponseCookie.from(CookieBearerTokenResolver.COOKIE_NAME, "")
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/")
            .maxAge(0)
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        ResponseCookie.from("spendwise_refresh", "")
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/api/auth")
            .maxAge(0)
            .build()
            .toString());
  }
}
