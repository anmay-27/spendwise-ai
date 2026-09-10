package com.anmay.spendwise.service;

import com.anmay.spendwise.dto.Requests.PaymentRequest;
import com.anmay.spendwise.dto.Responses.PaymentResponse;
import com.anmay.spendwise.repository.WalletRepository;
import com.anmay.spendwise.security.SessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IdempotentPaymentService {
  private final PaymentService payments;
  private final WalletRepository wallets;
  private final JdbcTemplate db;
  private final ObjectMapper json;

  public IdempotentPaymentService(
      PaymentService payments, WalletRepository wallets, JdbcTemplate db, ObjectMapper json) {
    this.payments = payments;
    this.wallets = wallets;
    this.db = db;
    this.json = json;
  }

  @Transactional
  public PaymentResponse pay(PaymentRequest request, String key) {
    UUID.fromString(key);
    wallets
        .lockByUserId(request.userId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
    try {
      String hash = SessionService.hash(json.writeValueAsString(request));
      var rows =
          db.queryForList(
              "select request_hash,response from payment_requests where user_id=? and"
                  + " request_key=?",
              request.userId(),
              key);
      if (!rows.isEmpty()) {
        if (!rows.get(0).get("request_hash").equals(hash))
          throw new ResponseStatusException(
              HttpStatus.CONFLICT, "Idempotency key already used for a different payment");
        return json.readValue(rows.get(0).get("response").toString(), PaymentResponse.class);
      }
      var result = payments.pay(request);
      db.update(
          "insert into payment_requests(user_id,request_key,request_hash,response) values(?,?,?,?)",
          request.userId(),
          key,
          hash,
          json.writeValueAsString(result));
      return result;
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      throw new IllegalStateException("Payment serialization failed", ex);
    }
  }
}
