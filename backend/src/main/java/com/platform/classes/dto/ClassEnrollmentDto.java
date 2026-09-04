package com.platform.classes.dto;

import java.time.Instant;
import java.util.UUID;

public record ClassEnrollmentDto(
        UUID id,
        UUID classId,
        UUID customerId,
        String status,
        Instant createdAt
) {
}
