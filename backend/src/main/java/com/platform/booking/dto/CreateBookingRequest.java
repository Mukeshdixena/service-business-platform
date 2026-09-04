package com.platform.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull(message = "must not be null") UUID serviceId,
        UUID staffId,
        @NotNull(message = "must not be null") @Future(message = "must be in the future") Instant startAt,
        String notes
) {
}
