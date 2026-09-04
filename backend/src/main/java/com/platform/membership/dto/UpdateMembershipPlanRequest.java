package com.platform.membership.dto;

import com.platform.membership.domain.MembershipDurationUnit;
import com.platform.membership.domain.MembershipPlanStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Partial update — all fields optional. */
public record UpdateMembershipPlanRequest(
        String name,
        String description,
        @DecimalMin(value = "0", message = "must be >= 0") BigDecimal price,
        String currency,
        @Positive(message = "must be > 0") Integer duration,
        MembershipDurationUnit durationUnit,
        MembershipPlanStatus status
) {
}
