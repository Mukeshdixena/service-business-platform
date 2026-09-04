package com.platform.catalog.dto;

import com.platform.catalog.domain.PricingUnit;
import com.platform.catalog.domain.ServiceBookingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateServiceRequest(
        @NotBlank(message = "must not be blank") String name,
        String description,
        @NotNull(message = "must not be null") @DecimalMin(value = "0.0", message = "must be >= 0") BigDecimal price,
        @NotBlank(message = "must not be blank") @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter ISO 4217 code") String currency,
        @Positive(message = "must be positive") Integer durationMinutes,
        @NotNull(message = "must not be null") ServiceBookingType bookingType,
        PricingUnit pricingUnit
) {
}
