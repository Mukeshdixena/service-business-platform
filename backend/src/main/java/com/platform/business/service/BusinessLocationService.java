package com.platform.business.service;

import com.platform.business.domain.BusinessLocation;
import com.platform.business.dto.CreateLocationRequest;
import com.platform.business.dto.LocationDto;
import com.platform.business.dto.UpdateLocationRequest;
import com.platform.business.repository.BusinessLocationRepository;
import com.platform.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BusinessLocationService {

    private final BusinessLocationRepository locationRepository;

    public BusinessLocationService(BusinessLocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @Transactional(readOnly = true)
    public List<LocationDto> list(UUID businessId) {
        return locationRepository.findByBusinessIdOrderByIsPrimaryDescCreatedAtAsc(businessId).stream()
                .map(BusinessLocationService::toDto)
                .toList();
    }

    @Transactional
    public LocationDto create(UUID businessId, CreateLocationRequest request) {
        BusinessLocation location = new BusinessLocation(businessId, request.label(), request.addressLine1(),
                request.addressLine2(), request.city(), request.state(), request.postalCode(), request.country(),
                request.latitude(), request.longitude(), request.isPrimary());
        return toDto(locationRepository.save(location));
    }

    @Transactional
    public LocationDto update(UUID businessId, UUID locationId, UpdateLocationRequest request) {
        BusinessLocation location = getOwned(businessId, locationId);
        location.update(request.label(), request.addressLine1(), request.addressLine2(), request.city(),
                request.state(), request.postalCode(), request.country(), request.latitude(), request.longitude(),
                request.isPrimary());
        return toDto(location);
    }

    @Transactional
    public void delete(UUID businessId, UUID locationId) {
        BusinessLocation location = getOwned(businessId, locationId);
        locationRepository.delete(location);
    }

    private BusinessLocation getOwned(UUID businessId, UUID locationId) {
        return locationRepository.findByIdAndBusinessId(locationId, businessId)
                .orElseThrow(() -> NotFoundException.of("Location", locationId));
    }

    public static LocationDto toDto(BusinessLocation l) {
        return new LocationDto(l.getId(), l.getBusinessId(), l.getLabel(), l.getAddressLine1(), l.getAddressLine2(),
                l.getCity(), l.getState(), l.getPostalCode(), l.getCountry(), l.getLatitude(), l.getLongitude(),
                l.isPrimary());
    }
}
