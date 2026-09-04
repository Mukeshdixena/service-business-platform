package com.platform.business.service;

import com.platform.business.domain.BusinessHours;
import com.platform.business.dto.BusinessHoursDto;
import com.platform.business.dto.UpsertBusinessHoursRequest;
import com.platform.business.repository.BusinessHoursRepository;
import com.platform.common.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BusinessHoursService {

    private final BusinessHoursRepository hoursRepository;

    public BusinessHoursService(BusinessHoursRepository hoursRepository) {
        this.hoursRepository = hoursRepository;
    }

    @Transactional(readOnly = true)
    public List<BusinessHoursDto> list(UUID businessId) {
        return hoursRepository.findByBusinessIdOrderByDayOfWeekAscOpenTimeAsc(businessId).stream()
                .map(BusinessHoursService::toDto)
                .toList();
    }

    /**
     * Full replace, per API_CONTRACT.md (PUT semantics) — the entire set of
     * hours rows for the business is deleted and re-created from the request.
     */
    @Transactional
    public List<BusinessHoursDto> replace(UUID businessId, UpsertBusinessHoursRequest request) {
        for (UpsertBusinessHoursRequest.Interval interval : request.hours()) {
            if (!interval.openTime().isBefore(interval.closeTime())) {
                throw new ValidationException("hours: openTime must be before closeTime for " + interval.dayOfWeek());
            }
        }
        hoursRepository.deleteByBusinessId(businessId);
        hoursRepository.flush();
        List<BusinessHours> saved = request.hours().stream()
                .map(i -> hoursRepository.save(new BusinessHours(businessId, i.dayOfWeek(), i.openTime(), i.closeTime())))
                .toList();
        return saved.stream().map(BusinessHoursService::toDto).toList();
    }

    public static BusinessHoursDto toDto(BusinessHours h) {
        return new BusinessHoursDto(h.getId(), h.getBusinessId(), h.getDayOfWeek(), h.getOpenTime(), h.getCloseTime());
    }
}
