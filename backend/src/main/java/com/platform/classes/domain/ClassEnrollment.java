package com.platform.classes.domain;

import com.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "class_enrollment")
public class ClassEnrollment extends BaseEntity {

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassEnrollmentStatus status = ClassEnrollmentStatus.ENROLLED;

    protected ClassEnrollment() {
    }

    public ClassEnrollment(UUID classId, UUID customerId, ClassEnrollmentStatus status) {
        this.classId = classId;
        this.customerId = customerId;
        this.status = status;
    }

    public void markAttended() {
        this.status = ClassEnrollmentStatus.ATTENDED;
    }

    public void markNoShow() {
        this.status = ClassEnrollmentStatus.NO_SHOW;
    }

    public void cancel() {
        this.status = ClassEnrollmentStatus.CANCELLED;
    }

    public UUID getClassId() {
        return classId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public ClassEnrollmentStatus getStatus() {
        return status;
    }
}
