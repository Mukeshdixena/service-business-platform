package com.platform.business.repository;

import com.platform.business.domain.BusinessMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessMembershipRepository extends JpaRepository<BusinessMembership, UUID> {

    Optional<BusinessMembership> findByBusinessIdAndUserId(UUID businessId, UUID userId);

    List<BusinessMembership> findByUserId(UUID userId);

    boolean existsByBusinessIdAndUserId(UUID businessId, UUID userId);
}
