package com.bookflow.backend.publicbooking.dto;

import com.bookflow.backend.publicbooking.BookingSlug;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Update the tenant public booking profile")
public record PublicProfileRequest(
		@NotBlank @Size(max = 150) String name,
		@Size(max = 30) String phone,
		@NotBlank
		@Size(min = BookingSlug.MIN_LENGTH, max = BookingSlug.MAX_LENGTH)
		@Pattern(regexp = BookingSlug.PATTERN)
		String slug,
		@Size(max = 500) String description,
		@NotNull Boolean publicBookingEnabled) {
}
