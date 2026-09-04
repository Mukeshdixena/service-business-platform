package com.platform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * CLAUDE_CODE.md §35: a customer may not have more than one concurrently-active
 * (WAITING/CALLED/SERVING) queue entry per business. Not one of the standard
 * codes listed in API_CONTRACT.md's error format section, so it is added here
 * following the same {timestamp,status,code,message,path} shape as every other
 * ApiException.
 */
public class QueueConflictException extends ApiException {

    public QueueConflictException(String message) {
        super(HttpStatus.CONFLICT, "QUEUE_CONFLICT", message);
    }

    public static QueueConflictException alreadyActive() {
        return new QueueConflictException("You already have an active queue entry at this business.");
    }
}
