package com.bookflow.backend.publicbooking.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Unauthenticated public appointment request")
public record PublicAppointmentRequest(
		@NotNull @Positive Long staffId,
		@NotNull @Positive Long serviceId,
		@Schema(
			description = "Appointment start time as an ISO-8601 UTC instant",
			example = "2026-09-14T09:00:00Z")
		@NotNull Instant startTime,
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Size(max = 30) String phone,
		@Schema(description = "Optional booking notes")
		String notes) {
}
