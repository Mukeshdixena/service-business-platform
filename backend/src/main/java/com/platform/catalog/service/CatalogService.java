package com.platform.catalog.service;

import com.platform.catalog.domain.Service;
import com.platform.catalog.domain.ServiceStatus;
import com.platform.catalog.dto.CreateServiceRequest;
import com.platform.catalog.dto.ServiceDto;
import com.platform.catalog.dto.ServicePublicDto;
import com.platform.catalog.dto.UpdateServiceRequest;
import com.platform.catalog.repository.ServiceRepository;
import com.platform.common.exception.NotFoundException;
import com.platform.common.exception.ValidationException;
import com.platform.common.pagination.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for the bookable-item ("service") catalog. Class name is fully
 * qualified with {@code @org.springframework.stereotype.Service} below because
 * the entity type in this package is also named {@code Service}.
 */
@org.springframework.stereotype.Service
public class CatalogService {

    private final ServiceRepository serviceRepository;

    public CatalogService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Transactional
    public ServiceDto create(UUID businessId, CreateServiceRequest request) {
        validateDuration(request.bookingType(), request.durationMinutes());
        validatePricingUnit(request.bookingType(), request.pricingUnit());
        Service service = new Service(businessId, request.name(), request.description(), request.price(),
                request.currency(), request.durationMinutes(), request.bookingType(), request.pricingUnit());
        return toDto(serviceRepository.save(service));
    }

    @Transactional(readOnly = true)
    public PageResponse<ServiceDto> list(UUID businessId, Pageable pageable) {
        Page<Service> page = serviceRepository.findByBusinessId(businessId, pageable);
        return PageResponse.of(page, CatalogService::toDto);
    }

    @Transactional(readOnly = true)
    public List<ServicePublicDto> listActivePublic(UUID businessId) {
        return serviceRepository.findByBusinessIdAndStatus(businessId, ServiceStatus.ACTIVE).stream()
                .map(CatalogService::toPublicDto)
                .toList();
    }

    @Transactional
    public ServiceDto update(UUID businessId, UUID serviceId, UpdateServiceRequest request) {
        Service service = getOwned(businessId, serviceId);
        var bookingType = request.bookingType() != null ? request.bookingType() : service.getBookingType();
        var duration = request.durationMinutes() != null ? request.durationMinutes() : service.getDurationMinutes();
        var pricingUnit = request.pricingUnit() != null ? request.pricingUnit() : service.getPricingUnit();
        validateDuration(bookingType, duration);
        validatePricingUnit(bookingType, pricingUnit);
        service.applyUpdate(request.name(), request.description(), request.price(), request.currency(),
                request.durationMinutes(), request.bookingType(), request.pricingUnit(), request.status());
        return toDto(service);
    }

    @Transactional
    public void softDelete(UUID businessId, UUID serviceId) {
        Service service = getOwned(businessId, serviceId);
        service.deactivate();
    }

    @Transactional(readOnly = true)
    public Service getOwned(UUID businessId, UUID serviceId) {
        return serviceRepository.findByIdAndBusinessId(serviceId, businessId)
                .orElseThrow(() -> NotFoundException.of("Service", serviceId));
    }

    private void validateDuration(com.platform.catalog.domain.ServiceBookingType bookingType, Integer durationMinutes) {
        if (bookingType == com.platform.catalog.domain.ServiceBookingType.APPOINTMENT
                && (durationMinutes == null || durationMinutes <= 0)) {
            throw new ValidationException("durationMinutes: must be > 0 for APPOINTMENT services");
        }
    }

    /** A RENTAL service's price is meaningless without knowing what period it is quoted per. */
    private void validatePricingUnit(com.platform.catalog.domain.ServiceBookingType bookingType,
                                      com.platform.catalog.domain.PricingUnit pricingUnit) {
        if (bookingType == com.platform.catalog.domain.ServiceBookingType.RENTAL && pricingUnit == null) {
            throw new ValidationException("pricingUnit: must be HOUR or DAY for RENTAL services");
        }
    }

    public static ServiceDto toDto(Service s) {
        return new ServiceDto(s.getId(), s.getBusinessId(), s.getName(), s.getDescription(), s.getPrice(),
                s.getCurrency(), s.getDurationMinutes(), s.getBookingType().name(), pricingUnitName(s),
                s.getStatus().name(), s.getCreatedAt(), s.getUpdatedAt());
    }

    public static ServicePublicDto toPublicDto(Service s) {
        return new ServicePublicDto(s.getId(), s.getBusinessId(), s.getName(), s.getDescription(), s.getPrice(),
                s.getCurrency(), s.getDurationMinutes(), s.getBookingType().name(), pricingUnitName(s),
                s.getStatus().name(), s.getCreatedAt(), s.getUpdatedAt());
    }

    private static String pricingUnitName(Service s) {
        return s.getPricingUnit() != null ? s.getPricingUnit().name() : null;
    }
}
