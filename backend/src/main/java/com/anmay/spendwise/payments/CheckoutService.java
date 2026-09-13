package com.anmay.spendwise.payments;

import com.anmay.spendwise.events.EventOutbox;
import com.anmay.spendwise.security.CurrentUserService;
import com.anmay.spendwise.security.SessionService;
import com.fasterxml.jackson.databind.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CheckoutService {
  public record OrderRequest(
      @NotBlank @Size(max = 160) String merchant,
      @NotNull @DecimalMin("1.00") @DecimalMax("100000.00") @Digits(integer = 6, fraction = 2)
          BigDecimal amount,
      @NotNull @Positive Long categoryId,
      @Size(max = 500) String notes) {}

  public record Verification(
      @NotBlank @Pattern(regexp = "pay_[A-Za-z0-9]{1,90}") String paymentId,
      @NotBlank @Pattern(regexp = "[a-fA-F0-9]{64}") String signature) {}

  private final JdbcTemplate db;
  private final CurrentUserService current;
  private final RazorpayClient razorpay;
  private final ObjectMapper json;
  private final EventOutbox events;

  public CheckoutService(
      JdbcTemplate db,
      CurrentUserService current,
      RazorpayClient razorpay,
      ObjectMapper json,
      EventOutbox events) {
    this.db = db;
    this.current = current;
    this.razorpay = razorpay;
    this.json = json;
    this.events = events;
  }

  public Map<String, Object> create(OrderRequest request, UUID requestKey) throws Exception {
    razorpay.requireConfigured();
    long uid = current.currentUserId();
    // Serializes retries for this owner. Provider creation alone never transfers money.
    db.queryForObject("select id from app_users where id=? for update", Long.class, uid);
    String hash = SessionService.hash(json.writeValueAsString(request));
    var previous =
        db.queryForList(
            "select * from checkout_orders where user_id=? and request_key=?",
            uid,
            requestKey.toString());
    if (!previous.isEmpty()) {
      if (!hash.equals(previous.getFirst().get("request_hash")))
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Payment request key was already used for different details");
      return view(previous.getFirst());
    }
    if (db.queryForObject(
            "select count(*) from categories where id=? and user_id=?",
            Integer.class,
            request.categoryId(),
            uid)
        != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
    String id = UUID.randomUUID().toString();
    long paise = request.amount().movePointRight(2).longValueExact();
    JsonNode order = razorpay.createOrder(paise, id);
    if (!order.path("id").asText().matches("order_[A-Za-z0-9]{1,90}")
        || order.path("amount").asLong() != paise
        || !"INR".equals(order.path("currency").asText()))
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Unexpected Razorpay order response");
    db.update(
        "insert into"
            + " checkout_orders(id,user_id,request_key,request_hash,provider_order_id,merchant,notes,category_id,amount_paise)"
            + " values(?,?,?,?,?,?,?,?,?)",
        id,
        uid,
        requestKey.toString(),
        hash,
        order.get("id").asText(),
        request.merchant().trim(),
        Objects.toString(request.notes(), ""),
        request.categoryId(),
        paise);
    return view(owned(id));
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> history() {
    return db
        .queryForList(
            "select * from checkout_orders where user_id=? order by created_at desc limit 50",
            current.currentUserId())
        .stream()
        .map(this::view)
        .toList();
  }

  public Map<String, Object> verify(String id, Verification verification) {
    var row = owned(id);
    razorpay.verifyCheckout(
        row.get("provider_order_id").toString(),
        verification.paymentId(),
        verification.signature());
    reconcile(row, razorpay.payment(verification.paymentId()));
    return view(owned(id));
  }

  public Map<String, Object> sync(String id) {
    var row = owned(id);
    var items = razorpay.payments(row.get("provider_order_id").toString()).path("items");
    // A retry may have failed after an earlier successful attempt. Success always wins.
    JsonNode selected = null;
    for (JsonNode payment : items) {
      if (selected == null || "authorized".equals(payment.path("status").asText()))
        selected = payment;
      if ("captured".equals(payment.path("status").asText())) {
        selected = payment;
        break;
      }
    }
    if (selected != null) reconcile(row, selected);
    return view(owned(id));
  }

  public void webhook(byte[] body, String signature) throws Exception {
    razorpay.verifyWebhook(body, signature);
    JsonNode event = json.readTree(body);
    if (!Set.of("payment.captured", "payment.authorized", "payment.failed", "order.paid")
        .contains(event.path("event").asText())) return;
    JsonNode payment = event.path("payload").path("payment").path("entity");
    String providerOrder = payment.path("order_id").asText();
    var rows =
        db.queryForList(
            "select * from checkout_orders where provider_order_id=? for update", providerOrder);
    // This merchant account may also receive events for other applications.
    if (rows.isEmpty()) return;
    reconcile(rows.getFirst(), razorpay.payment(payment.path("id").asText()));
  }

  private Map<String, Object> owned(String id) {
    var rows =
        db.queryForList(
            "select * from checkout_orders where id=? and user_id=? for update",
            id,
            current.currentUserId());
    if (rows.isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
    return rows.getFirst();
  }

  private void reconcile(Map<String, Object> row, JsonNode payment) {
    if (!row.get("provider_order_id").equals(payment.path("order_id").asText())
        || ((Number) row.get("amount_paise")).longValue() != payment.path("amount").asLong()
        || !"INR".equals(payment.path("currency").asText())
        || !payment.path("id").asText().matches("pay_[A-Za-z0-9]{1,90}"))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Payment does not match the stored order");
    if ("CAPTURED".equals(row.get("status"))) return;
    String status = payment.path("status").asText();
    if (!Set.of("captured", "authorized", "failed").contains(status)) return;
    if ("AUTHORIZED".equals(row.get("status")) && status.equals("failed")) return;
    Long tx = null;
    long uid = ((Number) row.get("user_id")).longValue();
    if (status.equals("captured")) {
      BigDecimal amount = BigDecimal.valueOf(((Number) row.get("amount_paise")).longValue(), 2);
      tx =
          db.queryForObject(
              "insert into"
                  + " expense_transactions(user_id,merchant_name,description,amount,category_id,ai_suggested_category,ai_confidence,status,occurred_at,provider_payment)"
                  + " select ?,?,?,?,?,name,1,'SUCCESSFUL',?,true from categories where id=?"
                  + " returning id",
              Long.class,
              uid,
              row.get("merchant"),
              row.get("notes"),
              amount,
              row.get("category_id"),
              LocalDateTime.now(),
              row.get("category_id"));
      events.append(
          "TRANSACTION_CREATED",
          tx,
          uid,
          Map.of(
              "amount",
              amount,
              "categoryId",
              row.get("category_id"),
              "type",
              "EXPENSE",
              "provider",
              "RAZORPAY_TEST"));
      events.append(
          "PAYMENT_CAPTURED",
          tx,
          uid,
          Map.of(
              "message",
              "Test payment of INR " + amount + " to " + row.get("merchant") + " confirmed",
              "orderId",
              row.get("id")));
    }
    db.update(
        "update checkout_orders set"
            + " status=?,provider_payment_id=?,transaction_id=?,updated_at=current_timestamp where"
            + " id=?",
        status.toUpperCase(Locale.ROOT),
        payment.path("id").asText(),
        tx,
        row.get("id"));
  }

  private Map<String, Object> view(Map<String, Object> row) {
    var result = new LinkedHashMap<String, Object>();
    for (String field :
        List.of(
            "id",
            "merchant",
            "notes",
            "status",
            "created_at",
            "provider_order_id",
            "provider_payment_id",
            "transaction_id")) result.put(field, row.get(field));
    result.put("amount", BigDecimal.valueOf(((Number) row.get("amount_paise")).longValue(), 2));
    result.put("amountPaise", row.get("amount_paise"));
    result.put("currency", "INR");
    result.put("testMode", true);
    result.put("keyId", razorpay.keyId());
    return result;
  }
}
