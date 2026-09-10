package com.bookflow.backend.publicbooking.dto;

import com.bookflow.backend.tenant.Tenant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Public business profile used by the booking page")
public record PublicBusinessResponse(
		String slug,
		String name,
		String phone,
		String description) {

	public static PublicBusinessResponse from(Tenant tenant) {
		return new PublicBusinessResponse(
				tenant.getSlug(),
				tenant.getName(),
				tenant.getPhone(),
				tenant.getDescription());
	}
}
