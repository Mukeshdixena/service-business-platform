package com.platform.catalog.repository;

import com.platform.catalog.domain.Service;
import com.platform.catalog.domain.ServiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRepository extends JpaRepository<Service, UUID> {

    Page<Service> findByBusinessId(UUID businessId, Pageable pageable);

    List<Service> findByBusinessIdAndStatus(UUID businessId, ServiceStatus status);

    Optional<Service> findByIdAndBusinessId(UUID id, UUID businessId);
}
