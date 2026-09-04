package com.platform.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code price}/{@code currency} are never accepted from the client — they are
 * always computed server-side from the service.
 *
 * <p>{@code endAt} is ignored for an APPOINTMENT-type service (computed from the
 * service's durationMinutes) and REQUIRED for a RENTAL-type service, whose
 * duration is customer-chosen. Correspondingly {@code resourceId} is required
 * for RENTAL and rejected for APPOINTMENT.
 */
public record CreateBookingRequest(
        @NotNull(message = "must not be null") UUID serviceId,
        UUID staffId,
        UUID resourceId,
        @NotNull(message = "must not be null") @Future(message = "must be in the future") Instant startAt,
        Instant endAt,
        String notes
) {
}
