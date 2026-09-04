package com.platform.resource.domain;

import com.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * A bookable physical asset (CLAUDE_CODE.md §11): a JCB, car, bike, room,
 * treatment chair. Explicitly NOT modelled as a staff member — a resource has
 * no user linkage, no service assignments and its own status vocabulary.
 */
@Entity
@Table(name = "resource")
public class Resource extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(nullable = false)
    private String name;

    @Column(length = 100)
    private String type;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    private String identifier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceStatus status = ResourceStatus.AVAILABLE;

    protected Resource() {
    }

    public Resource(UUID businessId, String name, String type, String description, String imageUrl, String identifier) {
        this.businessId = businessId;
        this.name = name;
        this.type = type;
        this.description = description;
        this.imageUrl = imageUrl;
        this.identifier = identifier;
    }

    public void applyUpdate(String name, String type, String description, String imageUrl, String identifier,
                             ResourceStatus status) {
        if (name != null) this.name = name;
        if (type != null) this.type = type;
        if (description != null) this.description = description;
        if (imageUrl != null) this.imageUrl = imageUrl;
        if (identifier != null) this.identifier = identifier;
        if (status != null) this.status = status;
    }

    /** Soft delete — a resource is never removed, since past bookings reference it. */
    public void markUnavailable() {
        this.status = ResourceStatus.UNAVAILABLE;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getIdentifier() {
        return identifier;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public boolean isBookable() {
        return status.isBookable();
    }
}
