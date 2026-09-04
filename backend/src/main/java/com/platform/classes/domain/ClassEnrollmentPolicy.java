package com.platform.classes.domain;

/**
 * The one place the "full class waitlists rather than rejects" rule lives
 * (CLAUDE_CODE.md §20, API_CONTRACT.md ClassEnrollmentDto). Pure and static so
 * it is directly unit-testable without a database.
 */
public final class ClassEnrollmentPolicy {

    private ClassEnrollmentPolicy() {
    }

    /**
     * @param enrolledCount current number of ENROLLED (not waitlisted/cancelled) enrollments
     * @param capacity      the class's configured capacity
     * @return ENROLLED while there is room, WAITLISTED once the class is full
     */
    public static ClassEnrollmentStatus statusFor(long enrolledCount, int capacity) {
        return enrolledCount < capacity ? ClassEnrollmentStatus.ENROLLED : ClassEnrollmentStatus.WAITLISTED;
    }
}
