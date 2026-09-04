package com.platform.membership.repository;

import com.platform.membership.domain.MembershipPlan;
import com.platform.membership.domain.MembershipPlanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, UUID> {

    Optional<MembershipPlan> findByIdAndBusinessId(UUID id, UUID businessId);

    Page<MembershipPlan> findByBusinessId(UUID businessId, Pageable pageable);

    List<MembershipPlan> findByBusinessIdAndStatus(UUID businessId, MembershipPlanStatus status);
}
