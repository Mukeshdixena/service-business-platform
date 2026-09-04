package com.platform.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Identical field set to {@link ServiceDto} — services have no private fields
 * (API_CONTRACT.md: "ServicePublicDto omits nothing extra here") — but only
 * ever populated for ACTIVE services on ACTIVE businesses. */
public record ServicePublicDto(
        UUID id,
        UUID businessId,
        String name,
        String description,
        BigDecimal price,
        String currency,
        Integer durationMinutes,
        String bookingType,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
