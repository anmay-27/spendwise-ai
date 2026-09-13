package com.anmay.spendwise.payments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class RazorpayClient {
  private final String keyId, secret, webhookSecret;
  private final ObjectMapper json;
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  public RazorpayClient(
      ObjectMapper json,
      @Value("${RAZORPAY_KEY_ID:}") String keyId,
      @Value("${RAZORPAY_KEY_SECRET:}") String secret,
      @Value("${RAZORPAY_WEBHOOK_SECRET:}") String webhookSecret) {
    this.json = json;
    this.keyId = keyId;
    this.secret = secret;
    this.webhookSecret = webhookSecret;
    if (!keyId.isBlank() && !keyId.startsWith("rzp_test_"))
      throw new IllegalStateException("Only Razorpay Test Mode keys are allowed");
  }

  public boolean configured() {
    return keyId.startsWith("rzp_test_") && !secret.isBlank();
  }

  public String keyId() {
    return configured() ? keyId : "";
  }

  public boolean webhookConfigured() {
    return configured() && !webhookSecret.isBlank();
  }

  public void requireConfigured() {
    if (!configured())
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Razorpay Test Mode is not configured. Add test keys to the backend environment.");
  }

  public JsonNode createOrder(long amount, String receipt) {
    return call("POST", "/orders", Map.of("amount", amount, "currency", "INR", "receipt", receipt));
  }

  public JsonNode payment(String id) {
    return call("GET", "/payments/" + safeId(id), null);
  }

  public JsonNode payments(String order) {
    return call("GET", "/orders/" + safeId(order) + "/payments", null);
  }

  private String safeId(String id) {
    if (id == null || !id.matches("[A-Za-z0-9_]{1,100}"))
      throw new IllegalArgumentException("Invalid provider identifier");
    return id;
  }

  private JsonNode call(String method, String path, Object body) {
    requireConfigured();
    try {
      String auth =
          Base64.getEncoder()
              .encodeToString((keyId + ":" + secret).getBytes(StandardCharsets.UTF_8));
      var request =
          HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1" + path))
              .timeout(Duration.ofSeconds(15))
              .header("Authorization", "Basic " + auth)
              .header("Content-Type", "application/json");
      if (method.equals("POST"))
        request.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
      else request.GET();
      var response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() / 100 != 2)
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY,
            "Razorpay could not complete the request. Check test credentials and retry status"
                + " verification.");
      return json.readTree(response.body());
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY,
          "Razorpay is temporarily unavailable; check payment status before retrying.");
    }
  }

  public void verifyCheckout(String order, String payment, String signature) {
    requireConfigured();
    verify((order + "|" + payment).getBytes(StandardCharsets.UTF_8), signature, secret);
  }

  public void verifyWebhook(byte[] body, String signature) {
    if (!webhookConfigured())
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Webhook is not configured");
    verify(body, signature, webhookSecret);
  }

  public static void verify(byte[] body, String signature, String secret) {
    try {
      if (signature == null || !signature.matches("[a-fA-F0-9]{64}"))
        throw new IllegalArgumentException();
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      if (!MessageDigest.isEqual(mac.doFinal(body), HexFormat.of().parseHex(signature)))
        throw new IllegalArgumentException();
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment signature");
    }
  }
}
