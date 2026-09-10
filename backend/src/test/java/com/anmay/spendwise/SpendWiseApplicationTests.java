package com.anmay.spendwise;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.anmay.spendwise.events.*;
import com.anmay.spendwise.finance.*;
import com.anmay.spendwise.service.*;
import com.fasterxml.jackson.databind.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
    properties = {
      "app.events.enabled=false",
      "spring.kafka.listener.auto-startup=false",
      "spring.kafka.admin.auto-create=false",
      "app.cache.enabled=false",
      "app.rate-limit.enabled=false",
      "app.demo.enabled=false",
      "app.security.jwt-secret=test-jwt-secret-which-is-at-least-thirty-two-bytes",
      "app.ml-service-key=test-key-which-is-at-least-thirty-two-bytes",
      "app.security.cookie-secure=false"
    })
@AutoConfigureMockMvc
class SpendWiseApplicationTests {
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate db;
  @Autowired FinanceEventConsumer consumer;
  @Autowired FinanceInsights finance;
  @Autowired AssistantService assistant;
  @org.springframework.test.context.bean.override.mockito.MockitoBean MlServiceClient ml;
  String email;
  Long uid;
  Long category;

  @BeforeEach
  void account() throws Exception {
    email = "user-" + UUID.randomUUID() + "@test.local";
    String body =
        json.writeValueAsString(
            Map.of("name", "Test", "email", email, "password", "testPassword123!"));
    var response =
        mvc.perform(
                post("/api/auth/register")
                    .with(secureCsrf())
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn();
    uid = json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
    category =
        db.queryForObject(
            "select id from categories where user_id=? and name='Food'", Long.class, uid);
    org.mockito.Mockito.when(
            ml.predict(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyList()))
        .thenReturn(new MlServiceClient.Prediction("Food", .95, "test"));
  }

  org.springframework.test.web.servlet.request.RequestPostProcessor secureCsrf() throws Exception {
    var response = mvc.perform(get("/api/auth/csrf")).andReturn().getResponse();
    var cookie = response.getCookie("XSRF-TOKEN");
    return request -> {
      var existing = request.getCookies();
      var cookies = new java.util.ArrayList<jakarta.servlet.http.Cookie>();
      if (existing != null) cookies.addAll(java.util.Arrays.asList(existing));
      cookies.add(cookie);
      request.setCookies(cookies.toArray(jakarta.servlet.http.Cookie[]::new));
      request.addHeader("X-XSRF-TOKEN", cookie.getValue());
      return request;
    };
  }

  String payload(String amount, String date) throws Exception {
    return json.writeValueAsString(
        Map.of(
            "merchant",
            "Test merchant",
            "amount",
            amount,
            "type",
            "EXPENSE",
            "categoryId",
            category,
            "notes",
            "test",
            "occurredAt",
            date,
            "recurring",
            false));
  }

  @Test
  void csrfBootstrapIgnoresExpiredAccessCookie() throws Exception {
    mvc.perform(get("/api/auth/csrf")
        .cookie(new jakarta.servlet.http.Cookie("spendwise_token", "expired.invalid.token")))
        .andExpect(status().isOk())
        .andExpect(cookie().exists("XSRF-TOKEN"));
  }

  @Test
  void ownershipAndOutbox() throws Exception {
    var result =
        mvc.perform(
                post("/api/transactions")
                    .with(user(email))
                    .with(secureCsrf())
                    .contentType("application/json")
                    .content(payload("120.25", LocalDateTime.now().minusDays(1).toString())))
            .andExpect(status().isCreated())
            .andReturn();
    Long id = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    assertEquals(
        1,
        db.queryForObject(
            "select count(*) from event_outbox where user_id=? and"
                + " event_type='TRANSACTION_CREATED'",
            Integer.class,
            uid));
    String other = "other-" + UUID.randomUUID() + "@test.local";
    mvc.perform(
            post("/api/auth/register")
                .with(secureCsrf())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of("name", "Other", "email", other, "password", "testPassword123!"))))
        .andExpect(status().isCreated());
    mvc.perform(get("/api/transactions").param("userId", uid.toString()).with(user(other)))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
    mvc.perform(
            patch("/api/transactions/" + id + "/category")
                .with(user(other))
                .with(secureCsrf())
                .contentType("application/json")
                .content("{\"categoryId\":" + category + "}"))
        .andExpect(status().isNotFound());
    mvc.perform(delete("/api/transactions/" + id).with(user(email)).with(secureCsrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  void monthBoundaryAndChat() {
    var now = YearMonth.now();
    insert("100", now.minusMonths(1).atDay(10).atStartOfDay(), "EXPENSE");
    insert("900", now.atDay(1).atStartOfDay(), "EXPENSE");
    assertEquals(
        new BigDecimal("100.00"), finance.sum(finance.rows(uid, now.minusMonths(1)), "EXPENSE"));
    assertTrue(
        assistant
            .answer(uid, "How much did I spend on food last month?")
            .answer()
            .contains("100.00"));
  }

  @Test
  void duplicateEventsAreIgnored() throws Exception {
    String body =
        json.writeValueAsString(
            new DomainEvent(
                UUID.randomUUID(), "TRANSACTION_CREATED", null, uid, Instant.now(), 1, Map.of()));
    consumer.consume(body);
    consumer.consume(body);
    assertEquals(
        1,
        db.queryForObject(
            "select count(*) from processed_events where event_id=?",
            Integer.class,
            json.readTree(body).get("eventId").asText()));
  }

  @Test
  void invalidAmountsAndUnauthenticatedAccess() throws Exception {
    mvc.perform(get("/api/transactions")).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/transactions")
                .with(user(email))
                .with(secureCsrf())
                .contentType("application/json")
                .content(payload("-1", LocalDateTime.now().minusHours(1).toString())))
        .andExpect(status().isBadRequest());
    assertEquals(
        0,
        db.queryForObject(
            "select count(*) from expense_transactions where user_id=?", Integer.class, uid));
  }

  @Test
  void refreshRotationRevokesPreviousAccess() throws Exception {
    var login =
        mvc.perform(
                post("/api/auth/login")
                    .with(secureCsrf())
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of("email", email, "password", "testPassword123!"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    var access = login.getCookie("spendwise_token");
    var refresh = login.getCookie("spendwise_refresh");
    assertNotNull(access);
    assertNotNull(refresh);
    mvc.perform(get("/api/auth/me").cookie(access)).andExpect(status().isOk());
    mvc.perform(post("/api/auth/refresh").cookie(refresh).with(secureCsrf()))
        .andExpect(status().isOk());
    mvc.perform(get("/api/auth/me").cookie(access)).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/auth/refresh").cookie(refresh).with(secureCsrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void incomeAndSubscriptionCalculations() {
    var now = YearMonth.now();
    insert("60000", now.atDay(1).atStartOfDay(), "INCOME");
    for (int i = 0; i < 3; i++)
      insert("649", now.minusMonths(i).atDay(1).atStartOfDay(), "EXPENSE");
    assertEquals(new BigDecimal("60000.00"), finance.analytics(uid, now).get("income"));
    assertEquals(1, finance.subscriptions(uid).size());
  }

  @Test
  void paymentRetriesDebitOnceAndRejectChangedPayload() throws Exception {
    db.update("update wallets set balance=1000 where user_id=?", uid);
    String key = UUID.randomUUID().toString(),
        body =
            json.writeValueAsString(
                Map.of("merchant", "Swiggy", "amount", 100, "description", "Dinner"));
    for (int i = 0; i < 2; i++)
      mvc.perform(
              post("/api/payments")
                  .with(user(email))
                  .with(secureCsrf())
                  .header("Idempotency-Key", key)
                  .contentType("application/json")
                  .content(body))
          .andExpect(status().isOk());
    assertEquals(
        new BigDecimal("900.00"),
        db.queryForObject("select balance from wallets where user_id=?", BigDecimal.class, uid));
    assertEquals(
        1,
        db.queryForObject(
            "select count(*) from expense_transactions where user_id=?", Integer.class, uid));
    mvc.perform(
            post("/api/payments")
                .with(user(email))
                .with(secureCsrf())
                .header("Idempotency-Key", key)
                .contentType("application/json")
                .content(body.replace("100", "200")))
        .andExpect(status().isConflict());
  }

  @Test
  void anomalyRequiresHistoryAndFlagsLargeDeviation() {
    var now = LocalDateTime.now();
    for (int i = 1; i <= 6; i++) insert("1000", now.minusDays(i), "EXPENSE");
    insert("42000", now.minusMinutes(1), "EXPENSE");
    assertEquals(1, finance.anomalies(uid, YearMonth.now()).size());
  }

  void insert(String amount, LocalDateTime date, String type) {
    db.update(
        "insert into"
            + " expense_transactions(user_id,merchant_name,amount,category_id,ai_suggested_category,ai_confidence,status,occurred_at,type)"
            + " values(?,?,?,?,?,1,'SUCCESSFUL',?,?)",
        uid,
        "Netflix",
        new BigDecimal(amount),
        category,
        "Food",
        java.sql.Timestamp.valueOf(date),
        type);
  }
}
