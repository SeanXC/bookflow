package com.bookflow.backend.publicbooking;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "bookflow.public-booking.rate-limit")
public record PublicBookingRateLimitProperties(
		@Positive int maxRequests,
		@NotNull Duration window) {
}
