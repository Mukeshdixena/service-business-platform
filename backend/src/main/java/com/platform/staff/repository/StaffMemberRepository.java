package com.platform.staff.repository;

import com.platform.staff.domain.StaffMember;
import com.platform.staff.domain.StaffStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffMemberRepository extends JpaRepository<StaffMember, UUID> {

    Page<StaffMember> findByBusinessId(UUID businessId, Pageable pageable);

    List<StaffMember> findByBusinessIdAndStatus(UUID businessId, StaffStatus status);

    Optional<StaffMember> findByIdAndBusinessId(UUID id, UUID businessId);
}
