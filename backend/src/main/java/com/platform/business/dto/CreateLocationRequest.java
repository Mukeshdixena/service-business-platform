package com.platform.business.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateLocationRequest(
        String label,
        @NotBlank(message = "must not be blank") String addressLine1,
        String addressLine2,
        @NotBlank(message = "must not be blank") String city,
        String state,
        String postalCode,
        @NotBlank(message = "must not be blank") String country,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean isPrimary
) {
}
