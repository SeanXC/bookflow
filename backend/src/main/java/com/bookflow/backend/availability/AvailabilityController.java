package com.bookflow.backend.availability;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookflow.backend.availability.dto.AvailabilityExceptionRequest;
import com.bookflow.backend.availability.dto.AvailabilityExceptionResponse;
import com.bookflow.backend.availability.dto.AvailableSlotResponse;
import com.bookflow.backend.availability.dto.WeeklyHoursRequest;
import com.bookflow.backend.availability.dto.WeeklyHoursResponse;
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
@RequestMapping("/api/staff/{staffId}/availability")
@RequiredArgsConstructor
@Tag(name = "Availability", description = "Staff working hours, exceptions, and bookable slots")
@ApiResponses({
	@ApiResponse(responseCode = "400", description = "Invalid request or overlapping window",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "401", description = "Authentication required",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "403", description = "Insufficient role or staff access",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "404", description = "Staff or availability resource not found",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "409", description = "Duplicate availability window",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class AvailabilityController {

	private final AvailabilityService availabilityService;
	private final CurrentUserProvider currentUserProvider;

	@GetMapping("/weekly-hours")
	@Operation(summary = "List weekly working hours for a staff member")
	public List<WeeklyHoursResponse> getWeeklyHours(@PathVariable Long staffId) {
		return availabilityService
				.getWeeklyHours(currentUserProvider.getTenantId(), staffId)
				.stream()
				.map(WeeklyHoursResponse::from)
				.toList();
	}

	@PostMapping("/weekly-hours")
	@Operation(summary = "Create a weekly working window")
	public ResponseEntity<WeeklyHoursResponse> createWeeklyHours(
			@PathVariable Long staffId,
			@Valid @RequestBody WeeklyHoursRequest request) {
		StaffWeeklyHours hours = availabilityService.createWeeklyHours(
				currentUserProvider.getTenantId(),
				staffId,
				request.dayOfWeek(),
				request.startTime(),
				request.endTime());
		return ResponseEntity.status(HttpStatus.CREATED).body(WeeklyHoursResponse.from(hours));
	}

	@PutMapping("/weekly-hours/{hoursId}")
	@Operation(summary = "Update a weekly working window")
	public WeeklyHoursResponse updateWeeklyHours(
			@PathVariable Long staffId,
			@PathVariable Long hoursId,
			@Valid @RequestBody WeeklyHoursRequest request) {
		return WeeklyHoursResponse.from(availabilityService.updateWeeklyHours(
				currentUserProvider.getTenantId(),
				staffId,
				hoursId,
				request.dayOfWeek(),
				request.startTime(),
				request.endTime()));
	}

	@DeleteMapping("/weekly-hours/{hoursId}")
	@Operation(summary = "Delete a weekly working window")
	public ResponseEntity<Void> deleteWeeklyHours(
			@PathVariable Long staffId,
			@PathVariable Long hoursId) {
		availabilityService.deleteWeeklyHours(
				currentUserProvider.getTenantId(),
				staffId,
				hoursId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/exceptions")
	@Operation(summary = "List availability exceptions in a date range")
	public List<AvailabilityExceptionResponse> getExceptions(
			@PathVariable Long staffId,
			@Parameter(description = "Inclusive start date")
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@Parameter(description = "Inclusive end date")
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return availabilityService
				.getExceptions(currentUserProvider.getTenantId(), staffId, from, to)
				.stream()
				.map(AvailabilityExceptionResponse::from)
				.toList();
	}

	@PostMapping("/exceptions")
	@Operation(summary = "Create an availability exception")
	public ResponseEntity<AvailabilityExceptionResponse> createException(
			@PathVariable Long staffId,
			@Valid @RequestBody AvailabilityExceptionRequest request) {
		StaffAvailabilityException exception = availabilityService.createException(
				currentUserProvider.getTenantId(),
				staffId,
				request.exceptionDate(),
				request.type(),
				request.startTime(),
				request.endTime(),
				request.note());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(AvailabilityExceptionResponse.from(exception));
	}

	@PutMapping("/exceptions/{exceptionId}")
	@Operation(summary = "Update an availability exception")
	public AvailabilityExceptionResponse updateException(
			@PathVariable Long staffId,
			@PathVariable Long exceptionId,
			@Valid @RequestBody AvailabilityExceptionRequest request) {
		return AvailabilityExceptionResponse.from(availabilityService.updateException(
				currentUserProvider.getTenantId(),
				staffId,
				exceptionId,
				request.exceptionDate(),
				request.type(),
				request.startTime(),
				request.endTime(),
				request.note()));
	}

	@DeleteMapping("/exceptions/{exceptionId}")
	@Operation(summary = "Delete an availability exception")
	public ResponseEntity<Void> deleteException(
			@PathVariable Long staffId,
			@PathVariable Long exceptionId) {
		availabilityService.deleteException(
				currentUserProvider.getTenantId(),
				staffId,
				exceptionId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/slots")
	@Operation(summary = "List bookable appointment slots for a service")
	public List<AvailableSlotResponse> getAvailableSlots(
			@PathVariable Long staffId,
			@Parameter(description = "Service whose duration defines slot length")
			@RequestParam Long serviceId,
			@Parameter(description = "Inclusive start date")
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@Parameter(description = "Inclusive end date")
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return availabilityService
				.getAvailableSlots(
						currentUserProvider.getTenantId(),
						staffId,
						serviceId,
						from,
						to)
				.stream()
				.map(AvailableSlotResponse::from)
				.toList();
	}
}
