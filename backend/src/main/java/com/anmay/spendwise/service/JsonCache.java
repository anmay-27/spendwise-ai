package com.anmay.spendwise.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class JsonCache {
  private final StringRedisTemplate redis;
  private final JdbcTemplate db;
  private final ObjectMapper json;
  private final boolean enabled;

  public JsonCache(
      StringRedisTemplate redis,
      JdbcTemplate db,
      ObjectMapper json,
      @Value("${app.cache.enabled:true}") boolean enabled) {
    this.redis = redis;
    this.db = db;
    this.json = json;
    this.enabled = enabled;
  }

  public Object get(String namespace, Long uid, String period, Supplier<Object> calculate) {
    if (!enabled) return calculate.get();
    Long version =
        db.queryForObject("select data_version from app_users where id=?", Long.class, uid);
    String key = namespace + ":" + uid + ":" + period + ":" + version;
    try {
      String value = redis.opsForValue().get(key);
      if (value != null) return json.readValue(value, Object.class);
    } catch (Exception ex) {
      org.slf4j.LoggerFactory.getLogger(getClass())
          .warn("Cache read unavailable namespace={}", namespace);
    }
    Object result = calculate.get();
    try {
      redis.opsForValue().set(key, json.writeValueAsString(result), Duration.ofSeconds(60));
    } catch (Exception ex) {
      org.slf4j.LoggerFactory.getLogger(getClass())
          .warn("Cache write unavailable namespace={}", namespace);
    }
    return result;
  }
}
