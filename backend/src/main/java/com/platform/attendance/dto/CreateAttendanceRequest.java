package com.platform.attendance.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** {@code customerId} is the customer's {@code CustomerProfile} id, as returned by every other DTO. */
public record CreateAttendanceRequest(
        @NotNull(message = "must not be null") UUID customerId
) {
}
