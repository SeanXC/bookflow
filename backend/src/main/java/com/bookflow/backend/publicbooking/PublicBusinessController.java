package com.bookflow.backend.publicbooking;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookflow.backend.availability.dto.AvailableSlotResponse;
import com.bookflow.backend.common.error.ApiErrorResponse;
import com.bookflow.backend.publicbooking.dto.PublicAppointmentRequest;
import com.bookflow.backend.publicbooking.dto.PublicAppointmentResponse;
import com.bookflow.backend.publicbooking.dto.PublicBusinessResponse;
import com.bookflow.backend.publicbooking.dto.PublicServiceResponse;
import com.bookflow.backend.publicbooking.dto.PublicStaffResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public/businesses")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Public booking", description = "Unauthenticated public booking APIs")
@ApiResponses({
	@ApiResponse(responseCode = "400", description = "Invalid date range or booking request",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "404", description = "Business, staff, or service not found",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "409", description = "Staff booking conflict",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "429", description = "Public booking rate limit exceeded",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class PublicBusinessController {

	private final PublicProfileService publicProfileService;
	private final PublicCatalogService publicCatalogService;
	private final PublicBookingService publicBookingService;

	@GetMapping("/{slug}")
	@Operation(summary = "Get a public business profile by booking slug")
	public PublicBusinessResponse getPublicBusiness(@PathVariable String slug) {
		return PublicBusinessResponse.from(publicProfileService.getPublicBusiness(slug));
	}

	@GetMapping("/{slug}/services")
	@Operation(summary = "List active public services")
	public List<PublicServiceResponse> getPublicServices(@PathVariable String slug) {
		return publicCatalogService.getPublicServices(slug).stream()
				.map(PublicServiceResponse::from)
				.toList();
	}

	@GetMapping("/{slug}/staff")
	@Operation(summary = "List active public staff")
	public List<PublicStaffResponse> getPublicStaff(@PathVariable String slug) {
		return publicCatalogService.getPublicStaff(slug).stream()
				.map(PublicStaffResponse::from)
				.toList();
	}

	@GetMapping("/{slug}/staff/{staffId}/slots")
	@Operation(summary = "List public bookable slots for a staff member and service")
	public List<AvailableSlotResponse> getPublicSlots(
			@PathVariable String slug,
			@PathVariable Long staffId,
			@RequestParam Long serviceId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return publicCatalogService
				.getPublicSlots(slug, staffId, serviceId, from, to)
				.stream()
				.map(AvailableSlotResponse::from)
				.toList();
	}

	@PostMapping("/{slug}/appointments")
	@Operation(summary = "Create a public appointment")
	public ResponseEntity<PublicAppointmentResponse> createPublicAppointment(
			@PathVariable String slug,
			@Valid @RequestBody PublicAppointmentRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(PublicAppointmentResponse.from(
						publicBookingService.createPublicAppointment(slug, request)));
	}
}
