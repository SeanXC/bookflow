package com.bookflow.backend.appointment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookflow.backend.appointment.dto.AppointmentRequest;
import com.bookflow.backend.appointment.dto.AppointmentResponse;
import com.bookflow.backend.appointment.dto.AppointmentStatusRequest;
import com.bookflow.backend.security.CurrentUserProvider;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

	private final AppointmentService appointmentService;
	private final CurrentUserProvider currentUserProvider;

	@GetMapping
	public Page<AppointmentResponse> getAllAppointments(Pageable pageable) {
		return appointmentService
				.getAllAppointments(currentUserProvider.getTenantId(), pageable)
				.map(AppointmentResponse::from);
	}

	@GetMapping("/{appointmentId}")
	public AppointmentResponse getAppointment(@PathVariable Long appointmentId) {
		return AppointmentResponse.from(appointmentService.getAppointment(
				currentUserProvider.getTenantId(),
				appointmentId));
	}

	@PostMapping
	public ResponseEntity<AppointmentResponse> createAppointment(
			@Valid @RequestBody AppointmentRequest request) {
		Appointment appointment = appointmentService.createAppointment(
				currentUserProvider.getTenantId(),
				request.customerId(),
				request.staffId(),
				request.serviceId(),
				request.startTime(),
				request.notes());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(AppointmentResponse.from(appointment));
	}

	@PutMapping("/{appointmentId}")
	public AppointmentResponse updateAppointment(
			@PathVariable Long appointmentId,
			@Valid @RequestBody AppointmentRequest request) {
		Appointment appointment = appointmentService.updateAppointment(
				currentUserProvider.getTenantId(),
				appointmentId,
				request.customerId(),
				request.staffId(),
				request.serviceId(),
				request.startTime(),
				request.notes());
		return AppointmentResponse.from(appointment);
	}

	@PatchMapping("/{appointmentId}/status")
	public AppointmentResponse updateStatus(
			@PathVariable Long appointmentId,
			@Valid @RequestBody AppointmentStatusRequest request) {
		return AppointmentResponse.from(appointmentService.updateStatus(
				currentUserProvider.getTenantId(),
				appointmentId,
				request.status()));
	}
}
