package com.platform.attendance.repository;

import com.platform.attendance.domain.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    Optional<Attendance> findByIdAndBusinessId(UUID id, UUID businessId);

    Page<Attendance> findByBusinessId(UUID businessId, Pageable pageable);

    Page<Attendance> findByBusinessIdAndCheckOutAtIsNull(UUID businessId, Pageable pageable);

    boolean existsByBusinessIdAndCustomerIdAndCheckOutAtIsNull(UUID businessId, UUID customerId);

    /**
     * Current occupancy (CLAUDE_CODE.md §18-19). Always a live COUNT over open
     * rows — the platform deliberately keeps no stored occupancy counter that
     * could drift out of sync with reality.
     */
    long countByBusinessIdAndCheckOutAtIsNull(UUID businessId);
}
