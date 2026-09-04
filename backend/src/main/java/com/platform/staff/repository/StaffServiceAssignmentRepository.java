package com.platform.staff.repository;

import com.platform.staff.domain.StaffServiceAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StaffServiceAssignmentRepository extends JpaRepository<StaffServiceAssignment, UUID> {

    List<StaffServiceAssignment> findByStaffId(UUID staffId);

    List<StaffServiceAssignment> findByServiceId(UUID serviceId);

    void deleteByStaffId(UUID staffId);
}
