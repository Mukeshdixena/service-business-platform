package com.platform.business.service;

import com.platform.business.domain.Business;
import com.platform.business.domain.BusinessMembership;
import com.platform.business.domain.BusinessMembershipRole;
import com.platform.business.domain.BusinessStatus;
import com.platform.business.dto.BusinessDto;
import com.platform.business.dto.BusinessMembershipDto;
import com.platform.business.dto.CreateBusinessRequest;
import com.platform.business.dto.UpdateBusinessRequest;
import com.platform.business.repository.BusinessMembershipRepository;
import com.platform.business.repository.BusinessRepository;
import com.platform.common.exception.InvalidStateTransitionException;
import com.platform.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessMembershipRepository membershipRepository;
    private final SlugGenerator slugGenerator;

    public BusinessService(BusinessRepository businessRepository,
                            BusinessMembershipRepository membershipRepository,
                            SlugGenerator slugGenerator) {
        this.businessRepository = businessRepository;
        this.membershipRepository = membershipRepository;
        this.slugGenerator = slugGenerator;
    }

    @Transactional
    public BusinessDto create(UUID ownerUserId, CreateBusinessRequest request) {
        String slug = (request.slug() == null || request.slug().isBlank())
                ? slugGenerator.generateUnique(request.name())
                : request.slug();
        if (businessRepository.existsBySlug(slug)) {
            throw new com.platform.common.exception.DuplicateSlugException(slug);
        }
        Business business = new Business(request.name(), slug, request.category());
        business.setDescription(request.description());
        business.setPhone(request.phone());
        business.setEmail(request.email());
        business.setLogoUrl(request.logoUrl());
        business.setCoverImageUrl(request.coverImageUrl());
        if (request.capabilities() != null) {
            business.setCapabilities(request.capabilities());
        }
        business.setMaxCapacity(request.maxCapacity());
        business = businessRepository.save(business);
        membershipRepository.save(new BusinessMembership(business.getId(), ownerUserId, BusinessMembershipRole.OWNER));
        return toDto(business);
    }

    @Transactional(readOnly = true)
    public BusinessDto getById(UUID businessId) {
        return toDto(getEntity(businessId));
    }

    @Transactional(readOnly = true)
    public Business getEntity(UUID businessId) {
        return businessRepository.findById(businessId)
                .orElseThrow(() -> NotFoundException.of("Business", businessId));
    }

    @Transactional
    public BusinessDto update(UUID businessId, UpdateBusinessRequest request) {
        Business business = getEntity(businessId);
        if (request.name() != null) business.setName(request.name());
        if (request.description() != null) business.setDescription(request.description());
        if (request.phone() != null) business.setPhone(request.phone());
        if (request.email() != null) business.setEmail(request.email());
        if (request.logoUrl() != null) business.setLogoUrl(request.logoUrl());
        if (request.coverImageUrl() != null) business.setCoverImageUrl(request.coverImageUrl());
        if (request.capabilities() != null) business.setCapabilities(request.capabilities());
        if (request.maxCapacity() != null) business.setMaxCapacity(request.maxCapacity());
        return toDto(business);
    }

    @Transactional
    public BusinessDto publish(UUID businessId) {
        Business business = getEntity(businessId);
        if (!business.isDraft()) {
            throw new InvalidStateTransitionException(
                    "Business must be in DRAFT status to publish (current: " + business.getStatus() + ")");
        }
        business.publish();
        return toDto(business);
    }

    @Transactional(readOnly = true)
    public List<BusinessMembershipDto> getMembershipsForUser(UUID userId) {
        List<BusinessMembership> memberships = membershipRepository.findByUserId(userId);
        return memberships.stream()
                .map(m -> new BusinessMembershipDto(m.getBusinessId(), businessRepository.findById(m.getBusinessId())
                        .map(Business::getName).orElse(""), m.getRole().name()))
                .collect(Collectors.toList());
    }

    public static BusinessDto toDto(Business b) {
        Set<String> capabilities = b.getCapabilities().stream().map(Enum::name).collect(Collectors.toSet());
        return new BusinessDto(b.getId(), b.getName(), b.getSlug(), b.getDescription(), b.getPhone(), b.getEmail(),
                b.getLogoUrl(), b.getCoverImageUrl(), b.getCategory().name(), capabilities, b.getStatus().name(),
                b.getVerificationStatus().name(), b.getMaxCapacity(), b.getCreatedAt(), b.getUpdatedAt());
    }
}
