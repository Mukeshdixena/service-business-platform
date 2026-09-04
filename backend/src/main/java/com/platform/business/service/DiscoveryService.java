package com.platform.business.service;

import com.platform.business.domain.Business;
import com.platform.business.domain.BusinessStatus;
import com.platform.business.dto.BusinessPublicDto;
import com.platform.business.dto.CategoryOptionDto;
import com.platform.business.repository.BusinessLocationRepository;
import com.platform.business.repository.BusinessRepository;
import com.platform.business.domain.BusinessCapability;
import com.platform.catalog.service.CatalogService;
import com.platform.common.exception.NotFoundException;
import com.platform.common.pagination.PageResponse;
import com.platform.membership.dto.MembershipPlanDto;
import com.platform.membership.service.MembershipPlanService;
import com.platform.staff.service.StaffMemberService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Assembles the public-facing view of a business for the {@code /discovery/**}
 * endpoints. Only ever reads businesses with {@code status = ACTIVE} and never
 * surfaces private fields (CLAUDE_CODE.md §41).
 */
@Service
public class DiscoveryService {

    private final BusinessRepository businessRepository;
    private final BusinessLocationRepository locationRepository;
    private final CatalogService catalogService;
    private final StaffMemberService staffMemberService;
    private final BusinessHoursService businessHoursService;
    private final MembershipPlanService membershipPlanService;

    public DiscoveryService(BusinessRepository businessRepository,
                             BusinessLocationRepository locationRepository,
                             CatalogService catalogService,
                             StaffMemberService staffMemberService,
                             BusinessHoursService businessHoursService,
                             MembershipPlanService membershipPlanService) {
        this.businessRepository = businessRepository;
        this.locationRepository = locationRepository;
        this.catalogService = catalogService;
        this.staffMemberService = staffMemberService;
        this.businessHoursService = businessHoursService;
        this.membershipPlanService = membershipPlanService;
    }

    @Transactional(readOnly = true)
    public List<CategoryOptionDto> listCategories() {
        return Arrays.stream(com.platform.business.domain.BusinessCategory.values())
                .map(c -> new CategoryOptionDto(c.name(), toLabel(c.name())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<BusinessPublicDto> search(String query, String category, String city, Pageable pageable) {
        Page<Business> page = businessRepository.search(BusinessStatus.ACTIVE.name(), category, query, city, pageable);
        return PageResponse.of(page, this::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public BusinessPublicDto getBySlug(String slug) {
        Business business = businessRepository.findBySlugAndStatus(slug, BusinessStatus.ACTIVE)
                .orElseThrow(() -> NotFoundException.of("Business", slug));
        return toFullDto(business);
    }

    private BusinessPublicDto toSummaryDto(Business business) {
        List<com.platform.business.dto.LocationDto> locations =
                locationRepository.findByBusinessIdOrderByIsPrimaryDescCreatedAtAsc(business.getId()).stream()
                        .map(BusinessLocationService::toDto)
                        .toList();
        return dto(business, locations, null, null, null);
    }

    private BusinessPublicDto toFullDto(Business business) {
        List<com.platform.business.dto.LocationDto> locations =
                locationRepository.findByBusinessIdOrderByIsPrimaryDescCreatedAtAsc(business.getId()).stream()
                        .map(BusinessLocationService::toDto)
                        .toList();
        var services = catalogService.listActivePublic(business.getId());
        var staff = staffMemberService.listActivePublic(business.getId());
        var hours = businessHoursService.list(business.getId());
        return dto(business, locations, services, staff, hours);
    }

    private BusinessPublicDto dto(Business b, List<com.platform.business.dto.LocationDto> locations,
                                   List<com.platform.catalog.dto.ServicePublicDto> services,
                                   List<com.platform.staff.dto.StaffPublicDto> staff,
                                   List<com.platform.business.dto.BusinessHoursDto> hours) {
        Set<String> capabilities = b.getCapabilities().stream().map(Enum::name).collect(Collectors.toSet());
        // Only way a customer can browse plans before purchasing (the management
        // listing endpoint is OWNER/STAFF-only) — always a list, never null, and
        // only ACTIVE plans, only when MEMBERSHIPS is enabled.
        List<MembershipPlanDto> membershipPlans = b.getCapabilities().contains(BusinessCapability.MEMBERSHIPS)
                ? membershipPlanService.listActivePublic(b.getId())
                : List.of();
        return new BusinessPublicDto(b.getId(), b.getName(), b.getSlug(), b.getDescription(), b.getPhone(),
                b.getEmail(), b.getLogoUrl(), b.getCoverImageUrl(), b.getCategory().name(), capabilities,
                locations, services, staff, hours, membershipPlans);
    }

    private String toLabel(String enumName) {
        String[] parts = enumName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(part.charAt(0)).append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
