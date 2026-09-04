package com.platform.customer.repository;

import com.platform.customer.domain.BusinessCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BusinessCustomerRepository extends JpaRepository<BusinessCustomer, UUID> {

    Optional<BusinessCustomer> findByBusinessIdAndCustomerProfileId(UUID businessId, UUID customerProfileId);

    boolean existsByBusinessIdAndCustomerProfileId(UUID businessId, UUID customerProfileId);
}
