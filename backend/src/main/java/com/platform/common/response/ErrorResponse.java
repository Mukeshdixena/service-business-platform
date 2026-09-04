package com.platform.common.response;

import java.time.Instant;

/**
 * Canonical error body shape for every non-2xx response, per API_CONTRACT.md.
 * Never carries a stack trace or internal exception message.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path
) {
    public static ErrorResponse of(int status, String code, String message, String path) {
        return new ErrorResponse(Instant.now(), status, code, message, path);
    }
}
