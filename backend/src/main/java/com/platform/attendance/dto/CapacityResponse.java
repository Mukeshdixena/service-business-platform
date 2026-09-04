package com.platform.attendance.dto;

/**
 * Live capacity snapshot (CLAUDE_CODE.md §19, API_CONTRACT.md CapacityResponse).
 * {@code current} is always a derived COUNT of open attendance records, never a
 * stored counter. When the business has no {@code maxCapacity} configured,
 * both {@code capacity} and {@code available} are null (unlimited/unknown), not 0.
 */
public record CapacityResponse(
        long current,
        Integer capacity,
        Integer available
) {

    public static CapacityResponse of(long current, Integer maxCapacity) {
        if (maxCapacity == null) {
            return new CapacityResponse(current, null, null);
        }
        // Never report a negative remaining capacity — an over-capacity venue
        // simply has 0 available.
        int available = (int) Math.max(0L, maxCapacity - current);
        return new CapacityResponse(current, maxCapacity, available);
    }
}
