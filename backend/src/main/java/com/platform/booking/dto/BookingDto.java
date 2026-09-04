package com.platform.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingDto(
        UUID id,
        UUID businessId,
        UUID customerId,
        UUID serviceId,
        UUID staffId,
        UUID resourceId,
        Instant startAt,
        Instant endAt,
        String status,
        BigDecimal price,
        String currency,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
