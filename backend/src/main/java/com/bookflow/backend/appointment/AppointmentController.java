package com.bookflow.backend.appointment;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookflow.backend.appointment.dto.AppointmentRequest;
import com.bookflow.backend.appointment.dto.AppointmentResponse;
import com.bookflow.backend.appointment.dto.AppointmentStatusRequest;
import com.bookflow.backend.common.error.ApiErrorResponse;
import com.bookflow.backend.security.CurrentUserProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Tenant-scoped appointment scheduling")
@ApiResponses({
	@ApiResponse(responseCode = "400", description = "Invalid request or status transition",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "401", description = "Authentication required",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "403", description = "Insufficient role or appointment access",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "404", description = "Appointment or related resource not found",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "409", description = "Staff booking conflict",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class AppointmentController {

	private final AppointmentService appointmentService;
	private final CurrentUserProvider currentUserProvider;

	@GetMapping
	@Operation(summary = "List and filter appointments")
	public Page<AppointmentResponse> getAllAppointments(
			@Parameter(description = "Filter by staff ID; ignored for STAFF users")
			@RequestParam(required = false) Long staffId,
			@Parameter(description = "Filter by appointment status")
			@RequestParam(required = false) AppointmentStatus status,
			@Parameter(description = "Inclusive UTC start-time lower bound")
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@Parameter(description = "Inclusive UTC start-time upper bound")
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
			@PageableDefault(
				size = 20,
				sort = {"startTime", "id"},
				direction = Sort.Direction.DESC) Pageable pageable) {
		return appointmentService
				.getAllAppointments(
						currentUserProvider.getTenantId(),
						staffId,
						status,
						from,
						to,
						pageable)
				.map(AppointmentResponse::from);
	}

	@GetMapping("/{appointmentId}")
	@Operation(summary = "Get an appointment")
	public AppointmentResponse getAppointment(@PathVariable Long appointmentId) {
		return AppointmentResponse.from(appointmentService.getAppointment(
				currentUserProvider.getTenantId(),
				appointmentId));
	}

	@PostMapping
	@Operation(summary = "Create an appointment")
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
	@Operation(summary = "Reschedule a confirmed appointment")
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
	@Operation(summary = "Complete or cancel a confirmed appointment")
	public AppointmentResponse updateStatus(
			@PathVariable Long appointmentId,
			@Valid @RequestBody AppointmentStatusRequest request) {
		return AppointmentResponse.from(appointmentService.updateStatus(
				currentUserProvider.getTenantId(),
				appointmentId,
				request.status()));
	}
}
