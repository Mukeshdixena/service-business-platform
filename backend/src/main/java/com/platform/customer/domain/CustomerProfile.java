package com.platform.customer.domain;

import com.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * The platform-wide customer identity for a User (CLAUDE_CODE.md §21): one per
 * user, shared across every business they transact with. Created lazily on
 * first use (either the GET /users/me/customer-profile call, or a business's
 * first booking) rather than eagerly for every registered user.
 */
@Entity
@Table(name = "customer_profile")
public class CustomerProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    private String phone;

    protected CustomerProfile() {
    }

    public CustomerProfile(UUID userId, String phone) {
        this.userId = userId;
        this.phone = phone;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
