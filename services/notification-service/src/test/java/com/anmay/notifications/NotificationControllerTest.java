package com.anmay.notifications;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.Cookie;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class NotificationControllerTest {
  @Test
  void readChecksCsrfAcrossMultipleCookieHeadersAndScopesUpdateToAuthenticatedUser() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext("/api/auth/me", exchange -> {
      byte[] body = "{\"id\":42}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, body.length);
      try (var output = exchange.getResponseBody()) { output.write(body); }
    });
    server.start();
    try {
      var db = mock(JdbcTemplate.class);
      String sql = "update notifications set is_read=true where event_id=? and user_id=?";
      when(db.update(sql, "event-1", 42L)).thenReturn(1);
      var controller = new NotificationController(db, new ObjectMapper(),
          "http://localhost:" + server.getAddress().getPort());
      var request = new MockHttpServletRequest();
      request.setCookies(new Cookie("spendwise_token", "session"), new Cookie("XSRF-TOKEN", "csrf"));
      request.addHeader("Cookie", "spendwise_token=session");
      request.addHeader("Cookie", "XSRF-TOKEN=csrf");
      request.addHeader("X-XSRF-TOKEN", "csrf");
      controller.read("event-1", request);
      verify(db).update(sql, "event-1", 42L);
      request.removeHeader("X-XSRF-TOKEN");
      var failure = assertThrows(ResponseStatusException.class, () -> controller.read("event-1", request));
      assertEquals(403, failure.getStatusCode().value());
      verifyNoMoreInteractions(db);
    } finally { server.stop(0); }
  }
}
