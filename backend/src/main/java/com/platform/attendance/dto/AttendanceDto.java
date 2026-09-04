package com.platform.attendance.dto;

import java.time.Instant;
import java.util.UUID;

public record AttendanceDto(
        UUID id,
        UUID businessId,
        UUID customerId,
        Instant checkInAt,
        Instant checkOutAt
) {
}
