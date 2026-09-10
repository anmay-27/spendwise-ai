package com.anmay.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
  private final JdbcTemplate db;
  private final ObjectMapper json;
  private final String core;
  private final HttpClient client =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

  public NotificationController(
      JdbcTemplate db, ObjectMapper json, @Value("${app.core-url}") String core) {
    this.db = db;
    this.json = json;
    this.core = core;
  }

  @GetMapping
  public Object list(HttpServletRequest request, @RequestParam(defaultValue = "0") int page) {
    if (page < 0 || page > 10000)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page");
    return db.queryForList(
        "select event_id,kind,message,is_read,created_at from notifications where user_id=? order"
            + " by created_at desc limit 50 offset ?",
        user(request),
        page * 50);
  }

  @PatchMapping("/{id}/read")
  public void read(@PathVariable String id, HttpServletRequest request) {
    // The core validates session revocation; this service checks the double-submit CSRF token.
    long uid = user(request);
    String csrf = request.getHeader("X-XSRF-TOKEN");
    var cookies = request.getCookies();
    if (csrf == null
        || cookies == null
        || java.util.Arrays.stream(cookies)
            .noneMatch(c -> "XSRF-TOKEN".equals(c.getName()) && csrf.equals(c.getValue())))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CSRF token required");
    if (db.update("update notifications set is_read=true where event_id=? and user_id=?", id, uid)
        == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
  }

  private long user(HttpServletRequest incoming) {
    try {
      var request =
          HttpRequest.newBuilder(URI.create(core + "/api/auth/me"))
              .timeout(Duration.ofSeconds(3))
              .GET();
      var cookieHeaders = incoming.getHeaders("Cookie");
      while (cookieHeaders.hasMoreElements()) request.header("Cookie", cookieHeaders.nextElement());
      if (incoming.getHeader("Authorization") != null)
        request.header("Authorization", incoming.getHeader("Authorization"));
      var response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200)
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in");
      return json.readTree(response.body()).path("id").asLong();
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Authentication service unavailable");
    }
  }
}
