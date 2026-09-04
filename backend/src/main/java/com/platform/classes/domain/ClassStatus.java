package com.platform.classes.domain;

/** CLAUDE_CODE.md §20 / API_CONTRACT.md ClassStatus. */
public enum ClassStatus {
    SCHEDULED,
    CANCELLED,
    COMPLETED;

    /** Only a SCHEDULED class accepts new enrollments (API_CONTRACT.md validation summary). */
    public boolean acceptsEnrollments() {
        return this == SCHEDULED;
    }
}
