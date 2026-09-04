package com.platform.queue.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateQueueEntryRequest(
        @NotNull(message = "must not be null") UUID serviceId,
        UUID staffId
) {
}
