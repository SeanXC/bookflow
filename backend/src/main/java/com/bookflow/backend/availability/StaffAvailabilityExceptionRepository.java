package com.bookflow.backend.availability;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffAvailabilityExceptionRepository
		extends JpaRepository<StaffAvailabilityException, Long> {

	Optional<StaffAvailabilityException> findByIdAndTenantId(Long id, Long tenantId);

	List<StaffAvailabilityException>
			findAllByTenantIdAndStaffIdAndExceptionDateBetweenOrderByExceptionDateAscStartTimeAsc(
					Long tenantId,
					Long staffId,
					LocalDate fromDate,
					LocalDate toDate);

	List<StaffAvailabilityException> findAllByTenantIdAndStaffIdAndExceptionDate(
			Long tenantId,
			Long staffId,
			LocalDate exceptionDate);
}
