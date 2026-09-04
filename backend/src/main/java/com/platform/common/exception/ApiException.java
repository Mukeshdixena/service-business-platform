package com.platform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for every domain-level exception that maps to a well-known
 * API_CONTRACT.md error {@code code}/HTTP status pair. Caught centrally by
 * {@link GlobalExceptionHandler}.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
