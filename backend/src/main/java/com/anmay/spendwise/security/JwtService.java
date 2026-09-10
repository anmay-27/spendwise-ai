package com.anmay.spendwise.security;

import com.anmay.spendwise.entity.AppUser;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final JwtEncoder jwtEncoder;
  private final Duration lifetime;

  public JwtService(
      JwtEncoder jwtEncoder,
      @Value("${app.security.jwt-expiration-minutes:15}") long expirationHours) {
    this.jwtEncoder = jwtEncoder;
    this.lifetime = Duration.ofMinutes(expirationHours);
  }

  public String createToken(AppUser user, String sessionId) {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer("spendwise")
            .issuedAt(now)
            .expiresAt(now.plus(lifetime))
            .subject(user.getEmail())
            .claim("uid", user.getId())
            .claim("sid", sessionId)
            .claim("name", user.getName())
            .claim("roles", List.of(user.getRole().name()))
            .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  public Duration lifetime() {
    return lifetime;
  }
}
