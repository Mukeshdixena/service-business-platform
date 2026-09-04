package com.platform.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentDto(
    UUID id,
    UUID businessId,
    UUID customerId,
    String referenceType,
    UUID referenceId,
    BigDecimal amount,
    String currency,
    String status,
    String provider,
    String providerReference,
    String createdAt,
    String updatedAt
) {}
