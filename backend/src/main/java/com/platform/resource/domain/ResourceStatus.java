package com.platform.resource.domain;

/** CLAUDE_CODE.md §11. MAINTENANCE/UNAVAILABLE resources cannot be booked. */
public enum ResourceStatus {
    AVAILABLE,
    RESERVED,
    IN_USE,
    MAINTENANCE,
    UNAVAILABLE;

    /** Whether a rental booking may be created against a resource in this status. */
    public boolean isBookable() {
        return this != MAINTENANCE && this != UNAVAILABLE;
    }
}
