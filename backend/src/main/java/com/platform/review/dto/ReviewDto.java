package com.platform.review.dto;

import java.util.UUID;

public record ReviewDto(
    UUID id,
    UUID businessId,
    UUID customerId,
    UUID bookingId,
    int rating,
    String comment,
    String status,
    String createdAt,
    String updatedAt
) {}
