package com.anmay.spendwise.controller;

import com.anmay.spendwise.dto.Requests.PaymentRequest;
import com.anmay.spendwise.dto.Responses.PaymentResponse;
import com.anmay.spendwise.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;
    public PaymentController(PaymentService paymentService) { this.paymentService = paymentService; }

    @PostMapping
    public PaymentResponse pay(@Valid @RequestBody PaymentRequest request) {
        return paymentService.pay(request);
    }
}
