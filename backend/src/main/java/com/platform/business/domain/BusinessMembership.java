package com.platform.business.domain;

import com.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

/**
 * The tenant-boundary join between a {@code User} and a {@code Business}
 * (CLAUDE_CODE.md §31/§39). Every business-scoped authorization check goes
 * through this table — never through trusting the {@code businessId} path
 * variable alone.
 */
@Entity
@Table(name = "business_membership",
        uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "user_id"}))
public class BusinessMembership extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinessMembershipRole role;

    protected BusinessMembership() {
    }

    public BusinessMembership(UUID businessId, UUID userId, BusinessMembershipRole role) {
        this.businessId = businessId;
        this.userId = userId;
        this.role = role;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public UUID getUserId() {
        return userId;
    }

    public BusinessMembershipRole getRole() {
        return role;
    }
}
