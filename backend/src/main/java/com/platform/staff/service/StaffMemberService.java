package com.platform.staff.service;

import com.platform.common.exception.NotFoundException;
import com.platform.staff.domain.StaffMember;
import com.platform.staff.domain.StaffServiceAssignment;
import com.platform.staff.dto.CreateStaffRequest;
import com.platform.staff.dto.StaffDto;
import com.platform.staff.dto.StaffPublicDto;
import com.platform.staff.dto.UpdateStaffRequest;
import com.platform.staff.repository.StaffMemberRepository;
import com.platform.staff.repository.StaffServiceAssignmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.platform.common.pagination.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class StaffMemberService {

    private final StaffMemberRepository staffMemberRepository;
    private final StaffServiceAssignmentRepository assignmentRepository;

    public StaffMemberService(StaffMemberRepository staffMemberRepository,
                               StaffServiceAssignmentRepository assignmentRepository) {
        this.staffMemberRepository = staffMemberRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional
    public StaffDto create(UUID businessId, CreateStaffRequest request) {
        StaffMember staff = new StaffMember(businessId, null, request.displayName(), request.title(),
                request.bio(), request.imageUrl());
        staff = staffMemberRepository.save(staff);
        assignServices(staff.getId(), request.serviceIds());
        return toDto(staff, serviceIdsFor(staff.getId()));
    }

    @Transactional(readOnly = true)
    public PageResponse<StaffDto> list(UUID businessId, Pageable pageable) {
        Page<StaffDto> page = staffMemberRepository.findByBusinessId(businessId, pageable)
                .map(staff -> toDto(staff, serviceIdsFor(staff.getId())));
        return PageResponse.of(page);
    }

    @Transactional(readOnly = true)
    public List<StaffPublicDto> listActivePublic(UUID businessId) {
        return staffMemberRepository.findByBusinessIdAndStatus(businessId, com.platform.staff.domain.StaffStatus.ACTIVE)
                .stream()
                .map(staff -> toPublicDto(staff, serviceIdsFor(staff.getId())))
                .toList();
    }

    @Transactional
    public StaffDto update(UUID businessId, UUID staffId, UpdateStaffRequest request) {
        StaffMember staff = getOwned(businessId, staffId);
        staff.applyUpdate(request.displayName(), request.title(), request.bio(), request.imageUrl(), request.status());
        if (request.serviceIds() != null) {
            assignmentRepository.deleteByStaffId(staffId);
            assignServices(staffId, request.serviceIds());
        }
        return toDto(staff, serviceIdsFor(staffId));
    }

    @Transactional
    public void softDelete(UUID businessId, UUID staffId) {
        StaffMember staff = getOwned(businessId, staffId);
        staff.deactivate();
    }

    @Transactional(readOnly = true)
    public StaffMember getOwned(UUID businessId, UUID staffId) {
        return staffMemberRepository.findByIdAndBusinessId(staffId, businessId)
                .orElseThrow(() -> NotFoundException.of("Staff member", staffId));
    }

    @Transactional(readOnly = true)
    public List<UUID> serviceIdsFor(UUID staffId) {
        return assignmentRepository.findByStaffId(staffId).stream()
                .map(StaffServiceAssignment::getServiceId)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StaffMember> findActiveAssignedToService(UUID businessId, UUID serviceId) {
        List<UUID> staffIds = assignmentRepository.findByServiceId(serviceId).stream()
                .map(StaffServiceAssignment::getStaffId)
                .toList();
        if (staffIds.isEmpty()) {
            return List.of();
        }
        return staffMemberRepository.findAllById(staffIds).stream()
                .filter(s -> s.getBusinessId().equals(businessId) && s.isActive())
                .toList();
    }

    private void assignServices(UUID staffId, List<UUID> serviceIds) {
        if (serviceIds == null) {
            return;
        }
        for (UUID serviceId : serviceIds) {
            assignmentRepository.save(new StaffServiceAssignment(staffId, serviceId));
        }
    }

    public static StaffDto toDto(StaffMember s, List<UUID> serviceIds) {
        return new StaffDto(s.getId(), s.getBusinessId(), s.getUserId(), s.getDisplayName(), s.getTitle(),
                s.getBio(), s.getImageUrl(), s.getStatus().name(), serviceIds);
    }

    public static StaffPublicDto toPublicDto(StaffMember s, List<UUID> serviceIds) {
        return new StaffPublicDto(s.getId(), s.getBusinessId(), s.getDisplayName(), s.getTitle(),
                s.getBio(), s.getImageUrl(), s.getStatus().name(), serviceIds);
    }
}
