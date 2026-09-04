package com.platform.queue.domain;

public enum QueueEntryStatus {
    WAITING,
    CALLED,
    SERVING,
    COMPLETED,
    SKIPPED,
    CANCELLED,
    NO_SHOW
}
