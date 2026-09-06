package com.bookflow.backend.appointment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookflow.backend.common.exception.AppointmentConflictException;
import com.bookflow.backend.common.exception.InvalidOperationException;
import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.customer.Customer;
import com.bookflow.backend.customer.CustomerRepository;
import com.bookflow.backend.security.CurrentUserProvider;
import com.bookflow.backend.service.ServiceRepository;
import com.bookflow.backend.staff.Staff;
import com.bookflow.backend.staff.StaffRepository;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;
import com.bookflow.backend.user.Role;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {

	private final AppointmentRepository appointmentRepository;
	private final TenantRepository tenantRepository;
	private final CustomerRepository customerRepository;
	private final StaffRepository staffRepository;
	private final ServiceRepository serviceRepository;
	private final CurrentUserProvider currentUserProvider;

	@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST', 'STAFF')")
	public Appointment getAppointment(Long tenantId, Long appointmentId) {
		Appointment appointment = getTenantAppointment(tenantId, appointmentId);
		ensureStaffCanAccess(appointment);
		return appointment;
	}

	@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST', 'STAFF')")
	public Page<Appointment> getAllAppointments(
			Long tenantId,
			Long staffId,
			AppointmentStatus status,
			Instant fromTime,
			Instant toTime,
			Pageable pageable) {
		validateTimeRange(fromTime, toTime);

		if (currentUserProvider.getRole() == Role.STAFF) {
			if (status != null || fromTime != null || toTime != null) {
				return appointmentRepository.findAllByTenantIdAndStaffUserIdAndFilters(
						tenantId,
						currentUserProvider.getUserId(),
						status,
						fromTime,
						toTime,
						pageable);
			}
			return appointmentRepository.findAllByTenantIdAndStaffUserId(
					tenantId,
					currentUserProvider.getUserId(),
					pageable);
		}
		if (staffId != null || status != null || fromTime != null || toTime != null) {
			return appointmentRepository.findAllByTenantIdAndFilters(
					tenantId,
					staffId,
					status,
					fromTime,
					toTime,
					pageable);
		}
		return appointmentRepository.findAllByTenantId(tenantId, pageable);
	}

	@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST')")
	@Transactional
	public Appointment createAppointment(
			Long tenantId,
			Long customerId,
			Long staffId,
			Long serviceId,
			Instant startTime,
			String notes) {
		Tenant tenant = tenantRepository.findById(tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
		Customer customer = customerRepository.findByIdAndTenantId(customerId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
		Staff staff = staffRepository.findForUpdateByIdAndTenantId(staffId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));
		com.bookflow.backend.service.Service service = serviceRepository
				.findByIdAndTenantId(serviceId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Service", serviceId));

		if (!staff.isActive()) {
			throw new InvalidOperationException("Inactive staff cannot receive appointments");
		}
		if (!service.isActive()) {
			throw new InvalidOperationException("Inactive services cannot be booked");
		}

		Instant endTime = startTime.plus(service.getDurationMinutes(), ChronoUnit.MINUTES);
		long conflicts = appointmentRepository.countConflictingAppointments(
				tenantId,
				staffId,
				AppointmentStatus.CANCELLED,
				startTime,
				endTime);
		if (conflicts > 0) {
			throw new AppointmentConflictException();
		}

		return appointmentRepository.save(new Appointment(
				tenant,
				customer,
				staff,
				service,
				startTime,
				endTime,
				notes));
	}

	@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST')")
	@Transactional
	public Appointment updateAppointment(
			Long tenantId,
			Long appointmentId,
			Long customerId,
			Long staffId,
			Long serviceId,
			Instant startTime,
			String notes) {
		Appointment appointment = getTenantAppointment(tenantId, appointmentId);
		if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
			throw new InvalidOperationException(
					"Only confirmed appointments can be rescheduled");
		}

		Customer customer = customerRepository.findByIdAndTenantId(customerId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
		Staff staff = staffRepository.findForUpdateByIdAndTenantId(staffId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));
		com.bookflow.backend.service.Service service = serviceRepository
				.findByIdAndTenantId(serviceId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Service", serviceId));

		if (!staff.isActive()) {
			throw new InvalidOperationException("Inactive staff cannot receive appointments");
		}
		if (!service.isActive()) {
			throw new InvalidOperationException("Inactive services cannot be booked");
		}

		Instant endTime = startTime.plus(service.getDurationMinutes(), ChronoUnit.MINUTES);
		long conflicts = appointmentRepository.countConflictingAppointmentsExcluding(
				tenantId,
				staffId,
				appointmentId,
				AppointmentStatus.CANCELLED,
				startTime,
				endTime);
		if (conflicts > 0) {
			throw new AppointmentConflictException();
		}

		appointment.updateDetails(customer, staff, service, startTime, endTime, notes);
		return appointment;
	}

	@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST', 'STAFF')")
	@Transactional
	public Appointment updateStatus(
			Long tenantId,
			Long appointmentId,
			AppointmentStatus newStatus) {
		Appointment appointment = getTenantAppointment(tenantId, appointmentId);
		ensureStaffCanAccess(appointment);

		if (appointment.getStatus() == newStatus) {
			return appointment;
		}
		if (appointment.getStatus() != AppointmentStatus.CONFIRMED
				|| newStatus == AppointmentStatus.CONFIRMED) {
			throw new InvalidOperationException(
					"Appointments can only transition from CONFIRMED to COMPLETED or CANCELLED");
		}

		appointment.updateStatus(newStatus);
		return appointment;
	}

	private Appointment getTenantAppointment(Long tenantId, Long appointmentId) {
		return appointmentRepository.findByIdAndTenantId(appointmentId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Appointment",
						appointmentId));
	}

	private void validateTimeRange(Instant fromTime, Instant toTime) {
		if (fromTime != null && toTime != null && fromTime.isAfter(toTime)) {
			throw new InvalidOperationException(
					"The appointment filter start time must not be after the end time");
		}
	}

	private void ensureStaffCanAccess(Appointment appointment) {
		if (currentUserProvider.getRole() != Role.STAFF) {
			return;
		}

		if (appointment.getStaff().getUser() == null
				|| !appointment.getStaff().getUser().getId()
						.equals(currentUserProvider.getUserId())) {
			throw new AccessDeniedException(
					"Staff users can only access their own appointments");
		}
	}
}
