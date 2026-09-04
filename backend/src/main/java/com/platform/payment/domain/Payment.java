package com.platform.payment.domain;

import com.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payment")
public class Payment extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "customer_profile_id", nullable = false)
    private UUID customerProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false)
    private PaymentReferenceType referenceType;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String provider;

    @Column(name = "provider_reference")
    private String providerReference;

    protected Payment() {
    }

    public Payment(UUID businessId, UUID customerProfileId, PaymentReferenceType referenceType,
                   UUID referenceId, BigDecimal amount, String currency) {
        this.businessId = businessId;
        this.customerProfileId = customerProfileId;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.amount = amount;
        this.currency = currency;
        this.status = PaymentStatus.PENDING;
    }

    public UUID getBusinessId() { return businessId; }
    public UUID getCustomerProfileId() { return customerProfileId; }
    public PaymentReferenceType getReferenceType() { return referenceType; }
    public UUID getReferenceId() { return referenceId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public String getProvider() { return provider; }
    public String getProviderReference() { return providerReference; }

    public void markSuccess(String provider, String providerReference) {
        this.status = PaymentStatus.SUCCESS;
        this.provider = provider;
        this.providerReference = providerReference;
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public void refund() {
        this.status = PaymentStatus.REFUNDED;
    }
}
