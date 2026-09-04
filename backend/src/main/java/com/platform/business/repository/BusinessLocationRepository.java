package com.platform.business.repository;

import com.platform.business.domain.BusinessLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessLocationRepository extends JpaRepository<BusinessLocation, UUID> {

    List<BusinessLocation> findByBusinessIdOrderByIsPrimaryDescCreatedAtAsc(UUID businessId);

    Optional<BusinessLocation> findByIdAndBusinessId(UUID id, UUID businessId);
}
