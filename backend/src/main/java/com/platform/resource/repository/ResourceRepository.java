package com.platform.resource.repository;

import com.platform.resource.domain.Resource;
import com.platform.resource.domain.ResourceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    Optional<Resource> findByIdAndBusinessId(UUID id, UUID businessId);

    Page<Resource> findByBusinessId(UUID businessId, Pageable pageable);

    List<Resource> findByBusinessIdAndStatusNot(UUID businessId, ResourceStatus status);
}
