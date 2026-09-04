package com.platform.membership.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MembershipPlanDto(
        UUID id,
        UUID businessId,
        String name,
        String description,
        BigDecimal price,
        String currency,
        Integer duration,
        String durationUnit,
        String status
) {
}
