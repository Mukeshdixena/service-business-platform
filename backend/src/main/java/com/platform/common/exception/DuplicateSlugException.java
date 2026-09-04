package com.platform.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateSlugException extends ApiException {

    public DuplicateSlugException(String slug) {
        super(HttpStatus.CONFLICT, "DUPLICATE_SLUG", "Slug already in use: " + slug);
    }
}
