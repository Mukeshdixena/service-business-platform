package com.platform.payment.service;

import com.platform.common.exception.NotFoundException;
import com.platform.common.exception.ValidationException;
import com.platform.customer.domain.CustomerProfile;
import com.platform.customer.service.CustomerProfileService;
import com.platform.payment.domain.Payment;
import com.platform.payment.domain.PaymentReferenceType;
import com.platform.payment.domain.PaymentStatus;
import com.platform.payment.dto.PaymentDto;
import com.platform.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Payment service interface — isolated behind a clean service boundary
 * (CLAUDE_CODE.md §22). For MVP, payments are simulated (no real provider
 * integration). The interface is designed so a real provider can be plugged in
 * later without changing callers.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CustomerProfileService customerProfileService;

    public PaymentService(PaymentRepository paymentRepository,
                          CustomerProfileService customerProfileService) {
        this.paymentRepository = paymentRepository;
        this.customerProfileService = customerProfileService;
    }

    @Transactional
    public Payment createPending(UUID userId, UUID businessId, PaymentReferenceType referenceType,
                                 UUID referenceId, java.math.BigDecimal amount, String currency) {
        CustomerProfile customer = customerProfileService.getOrCreateEntity(userId);
        Payment payment = new Payment(businessId, customer.getId(), referenceType, referenceId, amount, currency);
        return paymentRepository.save(payment);
    }

    /**
     * Simulates a successful payment. In a real implementation this would
     * call the payment provider's API and handle the response.
     */
    @Transactional
    public PaymentDto simulateSuccess(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> NotFoundException.of("Payment", paymentId));
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ValidationException("Only pending payments can be marked as successful.");
        }
        payment.markSuccess("SIMULATED", "SIM-" + UUID.randomUUID());
        return toDto(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public PaymentDto getById(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> NotFoundException.of("Payment", paymentId));
        return toDto(payment);
    }

    @Transactional(readOnly = true)
    public PaymentDto getByReference(String referenceType, UUID referenceId) {
        Payment payment = paymentRepository.findByReferenceTypeAndReferenceId(referenceType, referenceId)
                .orElseThrow(() -> NotFoundException.of("Payment for " + referenceType, referenceId));
        return toDto(payment);
    }

    public static PaymentDto toDto(Payment p) {
        return new PaymentDto(
                p.getId(),
                p.getBusinessId(),
                p.getCustomerProfileId(),
                p.getReferenceType().name(),
                p.getReferenceId(),
                p.getAmount(),
                p.getCurrency(),
                p.getStatus().name(),
                p.getProvider(),
                p.getProviderReference(),
                p.getCreatedAt().toString(),
                p.getUpdatedAt().toString()
        );
    }
}
