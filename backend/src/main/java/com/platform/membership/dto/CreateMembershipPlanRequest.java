package com.platform.membership.dto;

import com.platform.membership.domain.MembershipDurationUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateMembershipPlanRequest(
        @NotBlank(message = "must not be blank") String name,
        String description,
        @NotNull(message = "must not be null") @DecimalMin(value = "0", message = "must be >= 0") BigDecimal price,
        @NotBlank(message = "must not be blank") @Size(min = 3, max = 3, message = "must be a 3-letter ISO 4217 code") String currency,
        @NotNull(message = "must not be null") @Positive(message = "must be > 0") Integer duration,
        @NotNull(message = "must not be null") MembershipDurationUnit durationUnit
) {
}
