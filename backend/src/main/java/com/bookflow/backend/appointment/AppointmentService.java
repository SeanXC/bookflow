package com.bookflow.backend.appointment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookflow.backend.common.exception.AppointmentConflictException;
import com.bookflow.backend.common.exception.InvalidOperationException;
import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.customer.Customer;
import com.bookflow.backend.customer.CustomerRepository;
import com.bookflow.backend.service.ServiceRepository;
import com.bookflow.backend.staff.Staff;
import com.bookflow.backend.staff.StaffRepository;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;

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

	@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST')")
	public Appointment getAppointment(Long tenantId, Long appointmentId) {
		return appointmentRepository.findByIdAndTenantId(appointmentId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Appointment",
						appointmentId));
	}

	@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST')")
	public Page<Appointment> getAllAppointments(Long tenantId, Pageable pageable) {
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
}
