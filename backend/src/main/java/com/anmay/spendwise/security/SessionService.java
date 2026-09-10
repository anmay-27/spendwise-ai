package com.anmay.spendwise.security;

import com.anmay.spendwise.entity.AppUser;
import com.anmay.spendwise.repository.AppUserRepository;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {
  private final JdbcTemplate db;
  private final AppUserRepository users;
  private final JwtService jwt;

  public SessionService(JdbcTemplate db, AppUserRepository users, JwtService jwt) {
    this.db = db;
    this.users = users;
    this.jwt = jwt;
  }

  public record Tokens(String access, String refresh) {}

  @Transactional
  public Tokens create(AppUser user) {
    String sid = UUID.randomUUID().toString(), token = randomToken();
    db.update(
        "insert into refresh_sessions(id,user_id,token_hash,expires_at) values(?,?,?,?)",
        sid,
        user.getId(),
        hash(token),
        java.sql.Timestamp.from(Instant.now().plus(Duration.ofDays(14))));
    return new Tokens(jwt.createToken(user, sid), token);
  }

  @Transactional(noRollbackFor = BadCredentialsException.class)
  public Tokens rotate(String token) {
    var rows =
        db.queryForList(
            "select * from refresh_sessions where token_hash=? for update", hash(token));
    if (rows.isEmpty()) throw new BadCredentialsException("Invalid refresh token");
    var row = rows.get(0);
    Long uid = ((Number) row.get("user_id")).longValue();
    if ((Boolean) row.get("revoked")) {
      db.update("update refresh_sessions set revoked=true where user_id=?", uid);
      throw new BadCredentialsException("Refresh reuse detected");
    }
    if (((java.sql.Timestamp) row.get("expires_at")).toInstant().isBefore(Instant.now()))
      throw new BadCredentialsException("Refresh expired");
    db.update("update refresh_sessions set revoked=true where id=?", row.get("id"));
    return create(
        users.findById(uid).orElseThrow(() -> new BadCredentialsException("Account not found")));
  }

  public boolean active(String id) {
    return id != null
        && Boolean.TRUE.equals(
            db.queryForObject(
                "select count(*)>0 from refresh_sessions where id=? and revoked=false and"
                    + " expires_at>current_timestamp",
                Boolean.class,
                id));
  }

  @Transactional
  public void revoke(String sid, String refresh) {
    if (sid != null) db.update("update refresh_sessions set revoked=true where id=?", sid);
    if (refresh != null)
      db.update("update refresh_sessions set revoked=true where token_hash=?", hash(refresh));
  }

  public static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private String randomToken() {
    byte[] bytes = new byte[48];
    new SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
