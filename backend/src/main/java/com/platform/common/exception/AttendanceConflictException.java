package com.platform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * A customer may not have more than one open ({@code checkOutAt IS NULL})
 * attendance record per business (API_CONTRACT.md validation summary).
 *
 * <p>API_CONTRACT.md leaves the choice open ("reuse BOOKING_CONFLICT-style
 * handling or add an ATTENDANCE_CONFLICT code, your call, document whichever").
 * We add a dedicated {@code ATTENDANCE_CONFLICT} code, following the
 * {@link QueueConflictException}/{@code QUEUE_CONFLICT} precedent: a distinct
 * code lets the frontend map this to a specific message ("this member is
 * already checked in") rather than the generic booking-slot message, and it
 * keeps the 409 shape identical to every other ApiException.
 */
public class AttendanceConflictException extends ApiException {

    public AttendanceConflictException(String message) {
        super(HttpStatus.CONFLICT, "ATTENDANCE_CONFLICT", message);
    }

    public static AttendanceConflictException alreadyCheckedIn() {
        return new AttendanceConflictException("This customer is already checked in at this business.");
    }

    public static AttendanceConflictException alreadyCheckedOut() {
        return new AttendanceConflictException("This attendance record is already checked out.");
    }
}
