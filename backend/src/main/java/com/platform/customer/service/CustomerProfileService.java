package com.platform.customer.service;

import com.platform.customer.domain.BusinessCustomer;
import com.platform.customer.domain.CustomerProfile;
import com.platform.customer.dto.CustomerProfileDto;
import com.platform.customer.repository.BusinessCustomerRepository;
import com.platform.customer.repository.CustomerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerProfileService {

    private final CustomerProfileRepository customerProfileRepository;
    private final BusinessCustomerRepository businessCustomerRepository;

    public CustomerProfileService(CustomerProfileRepository customerProfileRepository,
                                   BusinessCustomerRepository businessCustomerRepository) {
        this.customerProfileRepository = customerProfileRepository;
        this.businessCustomerRepository = businessCustomerRepository;
    }

    @Transactional
    public CustomerProfileDto getOrCreateDto(UUID userId) {
        return toDto(getOrCreateEntity(userId));
    }

    @Transactional
    public CustomerProfile getOrCreateEntity(UUID userId) {
        return customerProfileRepository.findByUserId(userId)
                .orElseGet(() -> customerProfileRepository.save(new CustomerProfile(userId, null)));
    }

    /**
     * Ensures a {@code BusinessCustomer} relationship row exists for this
     * customer at this business — called on a customer's first booking
     * (CLAUDE_CODE.md §21), idempotent for every booking after that.
     */
    @Transactional
    public void ensureBusinessRelationship(UUID businessId, UUID customerProfileId) {
        if (!businessCustomerRepository.existsByBusinessIdAndCustomerProfileId(businessId, customerProfileId)) {
            businessCustomerRepository.save(new BusinessCustomer(businessId, customerProfileId));
        }
    }

    public static CustomerProfileDto toDto(CustomerProfile p) {
        return new CustomerProfileDto(p.getId(), p.getUserId(), p.getPhone(), p.getCreatedAt());
    }
}
