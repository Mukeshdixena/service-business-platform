package com.platform.staff.domain;

import com.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

/**
 * Join row expressing "this staff member can perform this service" (many-to-many).
 * A plain entity (rather than a Hibernate @ManyToMany collection) so inserts/
 * deletes on assignment changes are explicit and easy to reason about.
 */
@Entity
@Table(name = "staff_service", uniqueConstraints = @UniqueConstraint(columnNames = {"staff_id", "service_id"}))
public class StaffServiceAssignment extends BaseEntity {

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    protected StaffServiceAssignment() {
    }

    public StaffServiceAssignment(UUID staffId, UUID serviceId) {
        this.staffId = staffId;
        this.serviceId = serviceId;
    }

    public UUID getStaffId() {
        return staffId;
    }

    public UUID getServiceId() {
        return serviceId;
    }
}
