package com.bookflow.backend.common.error;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Consistent error response returned by the REST API")
public record ApiErrorResponse(
		@Schema(example = "409")
		int status,
		@Schema(example = "BOOKING_CONFLICT")
		String error,
		@Schema(
			example = "This staff member already has an appointment during the selected time.")
		String message,
		@Schema(example = "2026-09-12T14:00:00Z")
		Instant timestamp) {
}
