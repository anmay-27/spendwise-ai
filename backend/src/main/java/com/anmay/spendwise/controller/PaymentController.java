package com.anmay.spendwise.controller;

import com.anmay.spendwise.dto.Requests.PaymentRequest;
import com.anmay.spendwise.dto.Responses.PaymentResponse;
import com.anmay.spendwise.service.IdempotentPaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
  @org.springframework.beans.factory.annotation.Autowired
  private com.anmay.spendwise.security.CurrentUserService currentUser;

  private final IdempotentPaymentService paymentService;

  public PaymentController(IdempotentPaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @PostMapping
  public PaymentResponse pay(
      @Valid @RequestBody PaymentRequest request, @RequestHeader("Idempotency-Key") String key) {
    return paymentService.pay(
        new PaymentRequest(
            currentUser.currentUserId(),
            request.merchant(),
            request.amount(),
            request.description()),
        key);
  }
}
