package com.platform.customer.domain;

import com.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

/**
 * The per-business relationship record (CLAUDE_CODE.md §21/§39): "this
 * platform customer has interacted with this business". Auto-created the first
 * time a customer books at a given business. Deliberately minimal in this
 * phase — business-specific notes/status are a later-phase concern.
 */
@Entity
@Table(name = "business_customer",
        uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "customer_profile_id"}))
public class BusinessCustomer extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "customer_profile_id", nullable = false)
    private UUID customerProfileId;

    protected BusinessCustomer() {
    }

    public BusinessCustomer(UUID businessId, UUID customerProfileId) {
        this.businessId = businessId;
        this.customerProfileId = customerProfileId;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public UUID getCustomerProfileId() {
        return customerProfileId;
    }
}
