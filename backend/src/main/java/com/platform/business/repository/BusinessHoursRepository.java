package com.platform.business.repository;

import com.platform.business.domain.BusinessHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

public interface BusinessHoursRepository extends JpaRepository<BusinessHours, UUID> {

    List<BusinessHours> findByBusinessIdOrderByDayOfWeekAscOpenTimeAsc(UUID businessId);

    List<BusinessHours> findByBusinessIdAndDayOfWeekOrderByOpenTimeAsc(UUID businessId, DayOfWeek dayOfWeek);

    void deleteByBusinessId(UUID businessId);
}
