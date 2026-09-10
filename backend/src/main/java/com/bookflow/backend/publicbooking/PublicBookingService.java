package com.bookflow.backend.publicbooking;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookflow.backend.appointment.Appointment;
import com.bookflow.backend.appointment.AppointmentService;
import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.customer.Customer;
import com.bookflow.backend.customer.CustomerRepository;
import com.bookflow.backend.publicbooking.dto.PublicAppointmentRequest;
import com.bookflow.backend.service.ServiceRepository;
import com.bookflow.backend.staff.Staff;
import com.bookflow.backend.staff.StaffRepository;
import com.bookflow.backend.tenant.Tenant;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicBookingService {

	private final PublicProfileService publicProfileService;
	private final CustomerRepository customerRepository;
	private final StaffRepository staffRepository;
	private final ServiceRepository serviceRepository;
	private final AppointmentService appointmentService;

	@Transactional
	public Appointment createPublicAppointment(
			String slug,
			PublicAppointmentRequest request) {
		return bookGuestAppointment(publicProfileService.getPublicBusiness(slug), request);
	}

	@Transactional
	public Appointment bookGuestAppointment(
			Tenant tenant,
			PublicAppointmentRequest request) {
		staffRepository.findByIdAndTenantId(request.staffId(), tenant.getId())
				.filter(Staff::isActive)
				.orElseThrow(() -> new ResourceNotFoundException("Staff", request.staffId()));
		serviceRepository.findByIdAndTenantId(request.serviceId(), tenant.getId())
				.filter(com.bookflow.backend.service.Service::isActive)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Service",
						request.serviceId()));

		Customer customer = findOrCreateCustomer(tenant, request);
		return appointmentService.bookAppointment(
				tenant.getId(),
				customer.getId(),
				request.staffId(),
				request.serviceId(),
				request.startTime(),
				normalizeOptional(request.notes()));
	}

	private Customer findOrCreateCustomer(Tenant tenant, PublicAppointmentRequest request) {
		String firstName = request.firstName().trim();
		String lastName = request.lastName().trim();
		String email = request.email().trim();
		String phone = request.phone().trim();

		return customerRepository
				.findFirstByTenantIdAndEmailIgnoreCase(tenant.getId(), email)
				.map(existing -> {
					existing.updateDetails(
							firstName,
							lastName,
							email,
							phone,
							existing.getNotes());
					return existing;
				})
				.orElseGet(() -> customerRepository.save(new Customer(
						tenant,
						firstName,
						lastName,
						email,
						phone,
						null)));
	}

	private String normalizeOptional(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
