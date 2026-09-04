package com.platform.admin.dto;

public record PlatformStatsDto(
    long totalBusinesses,
    long activeBusinesses,
    long totalUsers,
    long totalBookings,
    long pendingReviews
) {}
