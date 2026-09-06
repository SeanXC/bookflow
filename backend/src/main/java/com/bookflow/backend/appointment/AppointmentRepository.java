package com.bookflow.backend.appointment;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

	@EntityGraph(attributePaths = {"customer", "staff", "service"})
	Optional<Appointment> findByIdAndTenantId(Long id, Long tenantId);

	@EntityGraph(attributePaths = {"customer", "staff", "service"})
	Page<Appointment> findAllByTenantId(Long tenantId, Pageable pageable);

	@EntityGraph(attributePaths = {"customer", "staff", "service"})
	Page<Appointment> findAllByTenantIdAndStaffUserId(
			Long tenantId,
			Long userId,
			Pageable pageable);

	@EntityGraph(attributePaths = {"customer", "staff", "service"})
	Page<Appointment> findAllByTenantIdAndCustomerId(
			Long tenantId,
			Long customerId,
			Pageable pageable);

	@EntityGraph(attributePaths = {"customer", "staff", "service"})
	@Query("""
			SELECT appointment
			FROM Appointment appointment
			WHERE appointment.tenant.id = :tenantId
			  AND appointment.staff.id = COALESCE(:staffId, appointment.staff.id)
			  AND appointment.status = COALESCE(:status, appointment.status)
			  AND appointment.startTime >= COALESCE(:fromTime, appointment.startTime)
			  AND appointment.startTime <= COALESCE(:toTime, appointment.startTime)
			""")
	Page<Appointment> findAllByTenantIdAndFilters(
			@Param("tenantId") Long tenantId,
			@Param("staffId") Long staffId,
			@Param("status") AppointmentStatus status,
			@Param("fromTime") Instant fromTime,
			@Param("toTime") Instant toTime,
			Pageable pageable);

	@EntityGraph(attributePaths = {"customer", "staff", "service"})
	@Query("""
			SELECT appointment
			FROM Appointment appointment
			WHERE appointment.tenant.id = :tenantId
			  AND appointment.staff.user.id = :userId
			  AND appointment.status = COALESCE(:status, appointment.status)
			  AND appointment.startTime >= COALESCE(:fromTime, appointment.startTime)
			  AND appointment.startTime <= COALESCE(:toTime, appointment.startTime)
			""")
	Page<Appointment> findAllByTenantIdAndStaffUserIdAndFilters(
			@Param("tenantId") Long tenantId,
			@Param("userId") Long userId,
			@Param("status") AppointmentStatus status,
			@Param("fromTime") Instant fromTime,
			@Param("toTime") Instant toTime,
			Pageable pageable);

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

	@Query("""
			SELECT COUNT(appointment)
			FROM Appointment appointment
			WHERE appointment.tenant.id = :tenantId
			  AND appointment.staff.id = :staffId
			  AND appointment.id <> :appointmentId
			  AND appointment.status <> :excludedStatus
			  AND appointment.startTime < :endTime
			  AND appointment.endTime > :startTime
			""")
	long countConflictingAppointmentsExcluding(
			@Param("tenantId") Long tenantId,
			@Param("staffId") Long staffId,
			@Param("appointmentId") Long appointmentId,
			@Param("excludedStatus") AppointmentStatus excludedStatus,
			@Param("startTime") Instant startTime,
			@Param("endTime") Instant endTime);
}
