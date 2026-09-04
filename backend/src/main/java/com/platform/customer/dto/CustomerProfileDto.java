package com.platform.customer.dto;

import java.time.Instant;
import java.util.UUID;

public record CustomerProfileDto(UUID id, UUID userId, String phone, Instant createdAt) {
}
