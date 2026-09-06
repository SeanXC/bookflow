package com.bookflow.backend.appointment.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Appointment creation or rescheduling request")
public record AppointmentRequest(
		@NotNull @Positive Long customerId,
		@NotNull @Positive Long staffId,
		@NotNull @Positive Long serviceId,
		@Schema(
			description = "Appointment start time as an ISO-8601 UTC instant",
			example = "2026-09-12T14:00:00Z")
		@NotNull Instant startTime,
		@Schema(description = "Optional booking notes")
		String notes) {
}
