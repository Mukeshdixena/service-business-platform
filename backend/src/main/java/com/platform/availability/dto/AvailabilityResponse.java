package com.platform.availability.dto;

import java.time.LocalDate;
import java.util.List;

public record AvailabilityResponse(
        String status,
        String type,
        LocalDate date,
        List<SlotDto> slots
) {
}
