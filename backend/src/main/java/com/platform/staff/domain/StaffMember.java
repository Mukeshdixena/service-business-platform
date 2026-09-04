package com.platform.staff.domain;

import com.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "staff_member")
public class StaffMember extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    /** Optional link to a platform User account; a staff profile can exist without one. */
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    private String title;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StaffStatus status = StaffStatus.ACTIVE;

    protected StaffMember() {
    }

    public StaffMember(UUID businessId, UUID userId, String displayName, String title, String bio, String imageUrl) {
        this.businessId = businessId;
        this.userId = userId;
        this.displayName = displayName;
        this.title = title;
        this.bio = bio;
        this.imageUrl = imageUrl;
    }

    public void applyUpdate(String displayName, String title, String bio, String imageUrl, StaffStatus status) {
        if (displayName != null) this.displayName = displayName;
        if (title != null) this.title = title;
        if (bio != null) this.bio = bio;
        if (imageUrl != null) this.imageUrl = imageUrl;
        if (status != null) this.status = status;
    }

    public void deactivate() {
        this.status = StaffStatus.INACTIVE;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTitle() {
        return title;
    }

    public String getBio() {
        return bio;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public StaffStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == StaffStatus.ACTIVE;
    }
}
