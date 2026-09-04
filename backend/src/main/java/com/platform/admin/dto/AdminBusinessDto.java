package com.platform.admin.dto;

import java.util.UUID;

public record AdminBusinessDto(
    UUID id,
    String name,
    String slug,
    String category,
    String status,
    String verificationStatus,
    String createdAt
) {}
