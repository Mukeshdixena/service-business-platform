package com.platform.common.exception;

import org.springframework.http.HttpStatus;

public class BookingConflictException extends ApiException {

    public BookingConflictException(String message) {
        super(HttpStatus.CONFLICT, "BOOKING_CONFLICT", message);
    }

    public static BookingConflictException slotTaken() {
        return new BookingConflictException("The selected time is no longer available.");
    }
}
