package com.platform.classes.domain;

import com.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A scheduled class (CLAUDE_CODE.md §20) — the API calls it a "Class"
 * (ClassDto / {@code /classes} endpoints); the Java type is named
 * {@code ClassSession} because {@code Class} would shadow {@code java.lang.Class}
 * throughout the codebase. The wire contract is unaffected.
 *
 * <p>{@code enrolledCount} is deliberately not a field: it is always derived by
 * counting ENROLLED enrollments (API_CONTRACT.md ClassDto).
 */
@Entity
@Table(name = "class_session")
public class ClassSession extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "staff_id")
    private UUID staffId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassStatus status = ClassStatus.SCHEDULED;

    protected ClassSession() {
    }

    public ClassSession(UUID businessId, String name, String description, UUID staffId, Instant startAt,
                         Instant endAt, Integer capacity) {
        this.businessId = businessId;
        this.name = name;
        this.description = description;
        this.staffId = staffId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.capacity = capacity;
    }

    public void applyUpdate(String name, String description, UUID staffId, Instant startAt, Instant endAt,
                             Integer capacity, ClassStatus status) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (staffId != null) this.staffId = staffId;
        if (startAt != null) this.startAt = startAt;
        if (endAt != null) this.endAt = endAt;
        if (capacity != null) this.capacity = capacity;
        if (status != null) this.status = status;
    }

    /** Soft delete — CANCELLED, never physically removed (enrollments reference it). */
    public void cancel() {
        this.status = ClassStatus.CANCELLED;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public UUID getStaffId() {
        return staffId;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public ClassStatus getStatus() {
        return status;
    }

    public boolean acceptsEnrollments() {
        return status.acceptsEnrollments();
    }
}
