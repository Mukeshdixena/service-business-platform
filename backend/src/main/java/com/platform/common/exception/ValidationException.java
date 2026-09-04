package com.platform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * For service-layer validation rules that Bean Validation annotations cannot express
 * (e.g. cross-field checks, uniqueness, business-hours containment).
 */
public class ValidationException extends ApiException {

    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
