package com.bookflow.backend.appointment;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

	Optional<Appointment> findByIdAndTenantId(Long id, Long tenantId);

	Page<Appointment> findAllByTenantId(Long tenantId, Pageable pageable);

	@Query("""
			SELECT COUNT(appointment)
			FROM Appointment appointment
			WHERE appointment.tenant.id = :tenantId
			  AND appointment.staff.id = :staffId
			  AND appointment.status <> :excludedStatus
			  AND appointment.startTime < :endTime
			  AND appointment.endTime > :startTime
			""")
	long countConflictingAppointments(
			@Param("tenantId") Long tenantId,
			@Param("staffId") Long staffId,
			@Param("excludedStatus") AppointmentStatus excludedStatus,
			@Param("startTime") Instant startTime,
			@Param("endTime") Instant endTime);
}
