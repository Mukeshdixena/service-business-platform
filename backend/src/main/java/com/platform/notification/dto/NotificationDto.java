package com.platform.notification.dto;

import java.util.UUID;

public record NotificationDto(
    UUID id,
    String type,
    String title,
    String message,
    String referenceType,
    UUID referenceId,
    boolean isRead,
    String createdAt
) {}
