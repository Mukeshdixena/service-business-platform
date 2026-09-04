package com.platform.catalog.dto;

import com.platform.catalog.domain.PricingUnit;
import com.platform.catalog.domain.ServiceBookingType;
import com.platform.catalog.domain.ServiceStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** All fields optional — partial update; null means "leave unchanged". */
public record UpdateServiceRequest(
        String name,
        String description,
        @DecimalMin(value = "0.0", message = "must be >= 0") BigDecimal price,
        @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter ISO 4217 code") String currency,
        @Positive(message = "must be positive") Integer durationMinutes,
        ServiceBookingType bookingType,
        PricingUnit pricingUnit,
        ServiceStatus status
) {
}
