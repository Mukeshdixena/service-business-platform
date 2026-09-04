package com.platform.classes.dto;

import com.platform.classes.domain.ClassStatus;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.UUID;

/** Partial update — all fields optional; null means "leave unchanged". */
public record UpdateClassRequest(
        String name,
        String description,
        UUID staffId,
        Instant startAt,
        Instant endAt,
        @Positive(message = "must be > 0") Integer capacity,
        ClassStatus status
) {
}
