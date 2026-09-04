package com.platform.payment.controller;

import com.platform.payment.dto.PaymentDto;
import com.platform.payment.service.PaymentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{id}")
    public PaymentDto getById(@PathVariable UUID id) {
        return paymentService.getById(id);
    }

    /**
     * Simulate a successful payment — MVP only. In production this would be
     * replaced by a webhook from the payment provider.
     */
    @PostMapping("/{id}/simulate-success")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentDto simulateSuccess(@PathVariable UUID id) {
        return paymentService.simulateSuccess(id);
    }
}
