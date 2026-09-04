package com.platform.classes.domain;

/** CLAUDE_CODE.md §20 / API_CONTRACT.md ClassEnrollmentStatus. */
public enum ClassEnrollmentStatus {
    ENROLLED,
    WAITLISTED,
    CANCELLED,
    ATTENDED,
    NO_SHOW;

    /** ENROLLED/WAITLISTED are the "still holds a place" statuses. */
    public boolean isActive() {
        return this == ENROLLED || this == WAITLISTED;
    }
}
