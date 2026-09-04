package com.platform.business.domain;

import com.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "business_location")
public class BusinessLocation extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    private String label;

    @Column(name = "address_line1", nullable = false)
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(nullable = false)
    private String city;

    private String state;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(nullable = false)
    private String country;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    protected BusinessLocation() {
    }

    public BusinessLocation(UUID businessId, String label, String addressLine1, String addressLine2,
                             String city, String state, String postalCode, String country,
                             BigDecimal latitude, BigDecimal longitude, boolean isPrimary) {
        this.businessId = businessId;
        this.label = label;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isPrimary = isPrimary;
    }

    public void update(String label, String addressLine1, String addressLine2, String city, String state,
                        String postalCode, String country, BigDecimal latitude, BigDecimal longitude,
                        boolean isPrimary) {
        this.label = label;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isPrimary = isPrimary;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public String getLabel() {
        return label;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public boolean isPrimary() {
        return isPrimary;
    }
}
