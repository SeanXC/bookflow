package com.bookflow.backend.common.error;

import java.time.Instant;

public record ApiErrorResponse(
		int status,
		String error,
		String message,
		Instant timestamp) {
}
