package com.platform.membership.dto;

import java.time.LocalDate;
import java.util.UUID;

public record MembershipDto(
        UUID id,
        UUID businessId,
        UUID customerId,
        UUID membershipPlanId,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        UUID paymentId
) {
}
