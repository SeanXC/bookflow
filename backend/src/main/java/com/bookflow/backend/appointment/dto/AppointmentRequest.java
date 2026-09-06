package com.bookflow.backend.appointment.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AppointmentRequest(
		@NotNull @Positive Long customerId,
		@NotNull @Positive Long staffId,
		@NotNull @Positive Long serviceId,
		@NotNull Instant startTime,
		String notes) {
}
