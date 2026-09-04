package com.platform.booking.dto;

/** Shared shape for both /cancel and /reject — API_CONTRACT.md gives both {@code { reason }}. */
public record CancelBookingRequest(String reason) {
}
