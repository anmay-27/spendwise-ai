package com.anmay.spendwise.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitFilter extends OncePerRequestFilter {
  private final StringRedisTemplate redis;
  private static final DefaultRedisScript<Long> SCRIPT =
      new DefaultRedisScript<>(
          "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],60) end;"
              + " return n",
          Long.class);

  public RateLimitFilter(StringRedisTemplate redis) {
    this.redis = redis;
  }

  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String path = req.getRequestURI();
    if (!req.getMethod().equals("POST")
        || !(path.startsWith("/api/auth/")
            || path.equals("/api/assistant/ask")
            || path.startsWith("/api/checkout/orders")
            || path.equals("/api/reports/generate"))) {
      chain.doFilter(req, res);
      return;
    }
    var auth =
        org.springframework.security.core.context.SecurityContextHolder.getContext()
            .getAuthentication();
    String identity =
        auth != null
                && auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt
            ? jwt.getClaimAsString("uid")
            : req.getRemoteAddr();
    try {
      Long n = redis.execute(SCRIPT, List.of("rate:" + path + ":" + SessionService.hash(identity)));
      if (n != null && n > 30) {
        res.setHeader("Retry-After", "60");
        com.anmay.spendwise.exception.ApiErrors.write(req, res, 429, "Rate limit exceeded");
        return;
      }
    } catch (org.springframework.data.redis.RedisConnectionFailureException ex) {
      com.anmay.spendwise.exception.ApiErrors.write(
          req, res, 503, "Rate limiter temporarily unavailable");
      return;
    }
    chain.doFilter(req, res);
  }
}
