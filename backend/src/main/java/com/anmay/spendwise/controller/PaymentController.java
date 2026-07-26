package com.anmay.spendwise.controller;

import com.anmay.spendwise.dto.Requests.ConfirmPaymentRequest;
import com.anmay.spendwise.dto.Requests.PaymentPreviewRequest;
import com.anmay.spendwise.dto.Responses.PaymentPreviewResponse;
import com.anmay.spendwise.dto.Responses.PaymentResponse;
import com.anmay.spendwise.security.CurrentUserService;
import com.anmay.spendwise.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;
    private final CurrentUserService currentUserService;

    public PaymentController(PaymentService paymentService,
                             CurrentUserService currentUserService) {
        this.paymentService = paymentService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/preview")
    public PaymentPreviewResponse preview(@Valid @RequestBody PaymentPreviewRequest request) {
        return paymentService.preview(currentUserService.currentUserId(), request);
    }

    @PostMapping("/confirm")
    public PaymentResponse confirm(@Valid @RequestBody ConfirmPaymentRequest request) {
        return paymentService.confirm(currentUserService.currentUserId(), request);
    }
}
