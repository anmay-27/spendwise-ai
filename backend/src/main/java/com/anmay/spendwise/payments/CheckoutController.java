package com.anmay.spendwise.payments;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
  private final CheckoutService service;
  private final RazorpayClient razorpay;

  public CheckoutController(CheckoutService service, RazorpayClient razorpay) {
    this.service = service;
    this.razorpay = razorpay;
  }

  @GetMapping("/config")
  public Object config() {
    return Map.of(
        "configured",
        razorpay.configured(),
        "testMode",
        true,
        "webhookConfigured",
        razorpay.webhookConfigured());
  }

  @GetMapping("/orders")
  public Object history() {
    return service.history();
  }

  @PostMapping("/orders")
  public Object create(
      @Valid @RequestBody CheckoutService.OrderRequest body,
      @RequestHeader("Idempotency-Key") UUID key)
      throws Exception {
    return service.create(body, key);
  }

  @PostMapping("/orders/{id}/verify")
  public Object verify(
      @PathVariable UUID id, @Valid @RequestBody CheckoutService.Verification body) {
    return service.verify(id.toString(), body);
  }

  @PostMapping("/orders/{id}/sync")
  public Object sync(@PathVariable UUID id) {
    return service.sync(id.toString());
  }

  @PostMapping("/webhook")
  public void webhook(
      @RequestBody byte[] body,
      @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature)
      throws Exception {
    service.webhook(body, signature);
  }
}
