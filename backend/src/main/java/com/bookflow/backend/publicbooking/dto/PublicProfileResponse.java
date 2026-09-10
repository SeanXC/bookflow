package com.bookflow.backend.publicbooking.dto;

import com.bookflow.backend.tenant.Tenant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Owner-facing public booking profile")
public record PublicProfileResponse(
		String slug,
		String name,
		String phone,
		String description,
		boolean publicBookingEnabled) {

	public static PublicProfileResponse from(Tenant tenant) {
		return new PublicProfileResponse(
				tenant.getSlug(),
				tenant.getName(),
				tenant.getPhone(),
				tenant.getDescription(),
				tenant.isPublicBookingEnabled());
	}
}
