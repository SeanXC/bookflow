package com.bookflow.backend.dashboard.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

public record DailyBookingResponse(
		@Schema(example = "2026-09-07")
		LocalDate date,
		@Schema(example = "12")
		long bookings) {
}
