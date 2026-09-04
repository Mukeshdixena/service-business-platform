package com.platform.business.domain;

import com.platform.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "business")
public class Business extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "text")
    private String description;

    private String phone;

    private String email;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinessCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinessStatus status = BusinessStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "business_capability", joinColumns = @JoinColumn(name = "business_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "capability", nullable = false)
    private Set<BusinessCapability> capabilities = EnumSet.noneOf(BusinessCapability.class);

    protected Business() {
    }

    public Business(String name, String slug, BusinessCategory category) {
        this.name = name;
        this.slug = slug;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public BusinessCategory getCategory() {
        return category;
    }

    public BusinessStatus getStatus() {
        return status;
    }

    public void publish() {
        this.status = BusinessStatus.ACTIVE;
    }

    public boolean isDraft() {
        return status == BusinessStatus.DRAFT;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public Set<BusinessCapability> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Set<BusinessCapability> capabilities) {
        this.capabilities = EnumSet.noneOf(BusinessCapability.class);
        this.capabilities.addAll(capabilities);
    }
}
