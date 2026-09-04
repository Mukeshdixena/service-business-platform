package com.platform.availability.dto;

import java.time.Instant;
import java.util.UUID;

public record SlotDto(Instant start, Instant end, boolean available, UUID staffId) {
}
