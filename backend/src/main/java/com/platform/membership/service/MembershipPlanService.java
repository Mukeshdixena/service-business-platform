package com.platform.membership.service;

import com.platform.common.exception.NotFoundException;
import com.platform.common.pagination.PageResponse;
import com.platform.membership.domain.MembershipPlan;
import com.platform.membership.domain.MembershipPlanStatus;
import com.platform.membership.dto.CreateMembershipPlanRequest;
import com.platform.membership.dto.MembershipPlanDto;
import com.platform.membership.dto.UpdateMembershipPlanRequest;
import com.platform.membership.repository.MembershipPlanRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service
public class MembershipPlanService {

    private final MembershipPlanRepository membershipPlanRepository;

    public MembershipPlanService(MembershipPlanRepository membershipPlanRepository) {
        this.membershipPlanRepository = membershipPlanRepository;
    }

    @Transactional
    public MembershipPlanDto create(UUID businessId, CreateMembershipPlanRequest request) {
        MembershipPlan plan = new MembershipPlan(businessId, request.name(), request.description(), request.price(),
                request.currency(), request.duration(), request.durationUnit());
        return toDto(membershipPlanRepository.save(plan));
    }

    @Transactional(readOnly = true)
    public PageResponse<MembershipPlanDto> list(UUID businessId, Pageable pageable) {
        Page<MembershipPlan> page = membershipPlanRepository.findByBusinessId(businessId, pageable);
        return PageResponse.of(page, MembershipPlanService::toDto);
    }

    @Transactional
    public MembershipPlanDto update(UUID businessId, UUID planId, UpdateMembershipPlanRequest request) {
        MembershipPlan plan = getOwned(businessId, planId);
        plan.applyUpdate(request.name(), request.description(), request.price(), request.currency(),
                request.duration(), request.durationUnit(), request.status());
        return toDto(plan);
    }

    /**
     * Public/discovery view (API_CONTRACT.md: {@code BusinessPublicDto.membershipPlans}) —
     * only ever ACTIVE plans, since that's the only status a customer should be
     * offered to purchase. Callers (the discovery service) are responsible for
     * only invoking this when the business has the MEMBERSHIPS capability.
     */
    @Transactional(readOnly = true)
    public List<MembershipPlanDto> listActivePublic(UUID businessId) {
        return membershipPlanRepository.findByBusinessIdAndStatus(businessId, MembershipPlanStatus.ACTIVE).stream()
                .map(MembershipPlanService::toDto)
                .toList();
    }

    @Transactional
    public void softDelete(UUID businessId, UUID planId) {
        MembershipPlan plan = getOwned(businessId, planId);
        plan.deactivate();
    }

    @Transactional(readOnly = true)
    public MembershipPlan getOwned(UUID businessId, UUID planId) {
        return membershipPlanRepository.findByIdAndBusinessId(planId, businessId)
                .orElseThrow(() -> NotFoundException.of("MembershipPlan", planId));
    }

    public static MembershipPlanDto toDto(MembershipPlan p) {
        return new MembershipPlanDto(p.getId(), p.getBusinessId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getCurrency(), p.getDuration(), p.getDurationUnit().name(), p.getStatus().name());
    }
}
