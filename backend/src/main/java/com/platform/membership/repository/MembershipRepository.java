package com.platform.membership.repository;

import com.platform.membership.domain.Membership;
import com.platform.membership.domain.MembershipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    Optional<Membership> findByIdAndBusinessId(UUID id, UUID businessId);

    Page<Membership> findByBusinessId(UUID businessId, Pageable pageable);

    Page<Membership> findByBusinessIdAndStatus(UUID businessId, MembershipStatus status, Pageable pageable);

    List<Membership> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
