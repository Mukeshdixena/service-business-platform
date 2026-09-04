package com.platform.business.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LocationDto(
        UUID id,
        UUID businessId,
        String label,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean isPrimary
) {
}
