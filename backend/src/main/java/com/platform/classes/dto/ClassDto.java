package com.platform.classes.dto;

import java.time.Instant;
import java.util.UUID;

/** {@code enrolledCount} is always derived (ENROLLED enrollments only), never stored. */
public record ClassDto(
        UUID id,
        UUID businessId,
        String name,
        String description,
        UUID staffId,
        Instant startAt,
        Instant endAt,
        Integer capacity,
        long enrolledCount,
        String status
) {
}
