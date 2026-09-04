package com.platform.queue.dto;

/** Customer-facing "where am I" view (API_CONTRACT.md, CLAUDE_CODE.md §36). */
public record QueueStatusResponse(
        String status,
        int peopleAhead,
        int estimatedWaitMinutes
) {
}
