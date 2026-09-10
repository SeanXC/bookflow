package com.bookflow.backend.availability;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffWeeklyHoursRepository extends JpaRepository<StaffWeeklyHours, Long> {

	Optional<StaffWeeklyHours> findByIdAndTenantId(Long id, Long tenantId);

	List<StaffWeeklyHours> findAllByTenantIdAndStaffIdOrderByDayOfWeekAscStartTimeAsc(
			Long tenantId,
			Long staffId);

	List<StaffWeeklyHours> findAllByTenantIdAndStaffIdAndDayOfWeek(
			Long tenantId,
			Long staffId,
			DayOfWeek dayOfWeek);
}
