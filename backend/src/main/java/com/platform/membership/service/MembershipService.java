package com.platform.membership.service;

import com.platform.business.domain.Business;
import com.platform.business.domain.BusinessCapability;
import com.platform.business.service.BusinessService;
import com.platform.common.exception.ForbiddenException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.exception.ValidationException;
import com.platform.common.pagination.PageResponse;
import com.platform.customer.service.CustomerProfileService;
import com.platform.membership.domain.Membership;
import com.platform.membership.domain.MembershipPlan;
import com.platform.membership.domain.MembershipStatus;
import com.platform.membership.dto.CreatePurchaseMembershipRequest;
import com.platform.membership.dto.MembershipDto;
import com.platform.membership.repository.MembershipRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for purchased memberships (CLAUDE_CODE.md §17). No real
 * payment provider is wired up this phase (CLAUDE_CODE.md §22) — purchasing a
 * plan creates the membership as ACTIVE immediately with paymentId always
 * null.
 *
 * <p>Expiry handling: rather than a scheduled job flipping ACTIVE -> EXPIRED at
 * midnight, every read path in this service calls {@link #effectiveStatus} to
 * lazily present an ACTIVE membership whose endDate has passed as EXPIRED, and
 * {@link #getOwned} persists that transition on the entity when it's touched.
 * This is the simplest option that satisfies the contract without introducing
 * a scheduler dependency for an MVP phase.
 */
@org.springframework.stereotype.Service
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final MembershipPlanService membershipPlanService;
    private final BusinessService businessService;
    private final CustomerProfileService customerProfileService;

    public MembershipService(MembershipRepository membershipRepository, MembershipPlanService membershipPlanService,
                              BusinessService businessService, CustomerProfileService customerProfileService) {
        this.membershipRepository = membershipRepository;
        this.membershipPlanService = membershipPlanService;
        this.businessService = businessService;
        this.customerProfileService = customerProfileService;
    }

    @Transactional
    public MembershipDto purchase(UUID businessId, UUID customerUserId, CreatePurchaseMembershipRequest request) {
        Business business = businessService.getEntity(businessId);
        if (!business.getCapabilities().contains(BusinessCapability.MEMBERSHIPS)) {
            throw new ValidationException("This business does not have the MEMBERSHIPS capability enabled.");
        }

        MembershipPlan plan = membershipPlanService.getOwned(businessId, request.membershipPlanId());
        if (!plan.isActive()) {
            throw new ValidationException("membershipPlanId: plan is not active.");
        }

        UUID customerProfileId = customerProfileService.getOrCreateEntity(customerUserId).getId();
        customerProfileService.ensureBusinessRelationship(businessId, customerProfileId);

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plus(plan.getDurationUnit().toPeriod(plan.getDuration()));

        Membership membership = new Membership(businessId, customerProfileId, plan.getId(), startDate, endDate);
        membership.transitionTo(MembershipStatus.ACTIVE);
        return toDto(membershipRepository.save(membership));
    }

    @Transactional(readOnly = true)
    public PageResponse<MembershipDto> listForBusiness(UUID businessId, MembershipStatus status, Pageable pageable) {
        Page<Membership> page = status != null
                ? membershipRepository.findByBusinessIdAndStatus(businessId, status, pageable)
                : membershipRepository.findByBusinessId(businessId, pageable);
        page.getContent().forEach(this::applyLazyExpiry);
        return PageResponse.of(page, MembershipService::toDto);
    }

    @Transactional(readOnly = true)
    public List<MembershipDto> listForCustomer(UUID customerProfileId) {
        List<Membership> memberships = membershipRepository.findByCustomerIdOrderByCreatedAtDesc(customerProfileId);
        memberships.forEach(this::applyLazyExpiry);
        return memberships.stream().map(MembershipService::toDto).toList();
    }

    @Transactional
    public MembershipDto freeze(UUID businessId, UUID id) {
        Membership membership = getOwned(businessId, id);
        membership.transitionTo(MembershipStatus.FROZEN);
        return toDto(membership);
    }

    @Transactional
    public MembershipDto reactivate(UUID businessId, UUID id) {
        Membership membership = getOwned(businessId, id);
        membership.transitionTo(MembershipStatus.ACTIVE);
        return toDto(membership);
    }

    @Transactional
    public MembershipDto cancelByBusiness(UUID businessId, UUID id) {
        Membership membership = getOwned(businessId, id);
        membership.transitionTo(MembershipStatus.CANCELLED);
        return toDto(membership);
    }

    /** Customer-initiated cancel — the caller must own the membership (via their customer profile). */
    @Transactional
    public MembershipDto cancelByCustomer(UUID id, UUID customerProfileId) {
        Membership membership = membershipRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Membership", id));
        if (!membership.getCustomerId().equals(customerProfileId)) {
            throw new ForbiddenException("You do not have permission to cancel this membership.");
        }
        applyLazyExpiry(membership);
        membership.transitionTo(MembershipStatus.CANCELLED);
        return toDto(membership);
    }

    @Transactional
    public Membership getOwned(UUID businessId, UUID id) {
        Membership membership = membershipRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> NotFoundException.of("Membership", id));
        applyLazyExpiry(membership);
        return membership;
    }

    /** Lazily flips ACTIVE -> EXPIRED once endDate has passed (see class javadoc). */
    private void applyLazyExpiry(Membership membership) {
        if (membership.isEffectivelyExpired(LocalDate.now())) {
            membership.markExpired();
        }
    }

    public static MembershipDto toDto(Membership m) {
        return new MembershipDto(m.getId(), m.getBusinessId(), m.getCustomerId(), m.getMembershipPlanId(),
                m.getStartDate(), m.getEndDate(), m.getStatus().name(), m.getPaymentId());
    }
}
