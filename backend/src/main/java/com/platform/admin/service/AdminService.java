package com.platform.admin.service;

import com.platform.business.domain.Business;
import com.platform.business.domain.BusinessStatus;
import com.platform.business.domain.VerificationStatus;
import com.platform.business.repository.BusinessRepository;
import com.platform.common.exception.NotFoundException;
import com.platform.common.exception.ValidationException;
import com.platform.common.pagination.PageResponse;
import com.platform.admin.dto.AdminBusinessDto;
import com.platform.admin.dto.PlatformStatsDto;
import com.platform.review.domain.ReviewStatus;
import com.platform.review.repository.ReviewRepository;
import com.platform.user.domain.User;
import com.platform.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminService {

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    public AdminService(BusinessRepository businessRepository,
                        UserRepository userRepository,
                        ReviewRepository reviewRepository) {
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public PlatformStatsDto getStats() {
        long totalBusinesses = businessRepository.count();
        long activeBusinesses = businessRepository.countByStatus(BusinessStatus.ACTIVE);
        long totalUsers = userRepository.count();
        long pendingReviews = reviewRepository.countByStatus(ReviewStatus.PENDING);
        return new PlatformStatsDto(totalBusinesses, activeBusinesses, totalUsers, 0, pendingReviews);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminBusinessDto> listBusinesses(String status, int page, int size) {
        Page<Business> businesses;
        if (status != null && !status.isBlank()) {
            BusinessStatus businessStatus = BusinessStatus.valueOf(status);
            businesses = businessRepository.findByStatus(businessStatus, PageRequest.of(page, size));
        } else {
            businesses = businessRepository.findAll(PageRequest.of(page, size));
        }
        return new PageResponse<>(
                businesses.getContent().stream().map(this::toBusinessDto).toList(),
                businesses.getNumber(),
                businesses.getSize(),
                businesses.getTotalElements(),
                businesses.getTotalPages(),
                businesses.hasNext()
        );
    }

    @Transactional
    public AdminBusinessDto verifyBusiness(UUID businessId, VerificationStatus verificationStatus) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> NotFoundException.of("Business", businessId));
        business.setVerificationStatus(verificationStatus);
        return toBusinessDto(businessRepository.save(business));
    }

    @Transactional
    public AdminBusinessDto suspendBusiness(UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> NotFoundException.of("Business", businessId));
        if (business.getStatus() == BusinessStatus.SUSPENDED) {
            throw new ValidationException("Business is already suspended.");
        }
        business.setStatus(BusinessStatus.SUSPENDED);
        return toBusinessDto(businessRepository.save(business));
    }

    @Transactional
    public AdminBusinessDto activateBusiness(UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> NotFoundException.of("Business", businessId));
        business.setStatus(BusinessStatus.ACTIVE);
        return toBusinessDto(businessRepository.save(business));
    }

    private AdminBusinessDto toBusinessDto(Business b) {
        return new AdminBusinessDto(
                b.getId(),
                b.getName(),
                b.getSlug(),
                b.getCategory() != null ? b.getCategory().name() : null,
                b.getStatus().name(),
                b.getVerificationStatus().name(),
                b.getCreatedAt().toString()
        );
    }
}
