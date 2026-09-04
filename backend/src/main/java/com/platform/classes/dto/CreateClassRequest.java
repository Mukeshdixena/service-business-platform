package com.platform.classes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.UUID;

public record CreateClassRequest(
        @NotBlank(message = "must not be blank") String name,
        String description,
        UUID staffId,
        @NotNull(message = "must not be null") Instant startAt,
        @NotNull(message = "must not be null") Instant endAt,
        @NotNull(message = "must not be null") @Positive(message = "must be > 0") Integer capacity
) {
}
