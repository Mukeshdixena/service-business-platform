package com.platform.classes.repository;

import com.platform.classes.domain.ClassEnrollment;
import com.platform.classes.domain.ClassEnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, UUID> {

    Optional<ClassEnrollment> findByIdAndClassId(UUID id, UUID classId);

    List<ClassEnrollment> findByClassIdOrderByCreatedAtAsc(UUID classId);

    List<ClassEnrollment> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    Optional<ClassEnrollment> findByClassIdAndCustomerIdAndStatusIn(UUID classId, UUID customerId,
                                                                     Collection<ClassEnrollmentStatus> statuses);

    /** Derived {@code enrolledCount} — ENROLLED only, never a stored column. */
    long countByClassIdAndStatus(UUID classId, ClassEnrollmentStatus status);
}
