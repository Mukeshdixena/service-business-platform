package com.platform.business.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record BusinessHoursDto(
        UUID id,
        UUID businessId,
        DayOfWeek dayOfWeek,
        @JsonFormat(pattern = "HH:mm") LocalTime openTime,
        @JsonFormat(pattern = "HH:mm") LocalTime closeTime
) {
}
