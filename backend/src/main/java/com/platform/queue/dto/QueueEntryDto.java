package com.platform.queue.dto;

import java.time.Instant;
import java.util.UUID;

public record QueueEntryDto(
        UUID id,
        UUID businessId,
        UUID customerId,
        UUID serviceId,
        UUID staffId,
        int position,
        String status,
        Instant joinedAt,
        Instant calledAt,
        Instant startedAt,
        Instant completedAt,
        int estimatedWaitMinutes
) {
}
