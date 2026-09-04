package com.platform.business.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record UpsertBusinessHoursRequest(
        @NotEmpty(message = "must contain at least one interval") @Valid List<Interval> hours
) {
    public record Interval(
            @NotNull(message = "must not be null") DayOfWeek dayOfWeek,
            @NotNull(message = "must not be null") @JsonFormat(pattern = "HH:mm") LocalTime openTime,
            @NotNull(message = "must not be null") @JsonFormat(pattern = "HH:mm") LocalTime closeTime
    ) {
    }
}
