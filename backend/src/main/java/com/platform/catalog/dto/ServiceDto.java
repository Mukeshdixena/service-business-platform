package com.platform.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ServiceDto(
        UUID id,
        UUID businessId,
        String name,
        String description,
        BigDecimal price,
        String currency,
        Integer durationMinutes,
        String bookingType,
        String pricingUnit,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
