package com.platform.queue.domain;

import com.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A customer waiting for walk-in/queue-type service (CLAUDE_CODE.md §15).
 * {@code position} is intentionally NOT a column here — it is never stored as
 * authoritative state; it is always derived at read time by
 * {@code QueueService} from {@code joinedAt} ordering over the business's
 * currently WAITING/CALLED entries.
 */
@Entity
@Table(name = "queue_entry")
public class QueueEntry extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "staff_id")
    private UUID staffId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueEntryStatus status = QueueEntryStatus.WAITING;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "called_at")
    private Instant calledAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected QueueEntry() {
    }

    public QueueEntry(UUID businessId, UUID customerId, UUID serviceId, UUID staffId) {
        this.businessId = businessId;
        this.customerId = customerId;
        this.serviceId = serviceId;
        this.staffId = staffId;
        this.joinedAt = Instant.now();
    }

    /** Server-enforced transition — see {@link QueueStateMachine}. Never call setStatus directly. */
    public void transitionTo(QueueEntryStatus target) {
        QueueStateMachine.requireTransition(this.status, target);
        this.status = target;
        Instant now = Instant.now();
        switch (target) {
            case CALLED -> this.calledAt = now;
            case SERVING -> this.startedAt = now;
            case COMPLETED -> this.completedAt = now;
            default -> { /* no timestamp tracked for SKIPPED/CANCELLED/NO_SHOW */ }
        }
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public UUID getStaffId() {
        return staffId;
    }

    public QueueEntryStatus getStatus() {
        return status;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public Instant getCalledAt() {
        return calledAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
