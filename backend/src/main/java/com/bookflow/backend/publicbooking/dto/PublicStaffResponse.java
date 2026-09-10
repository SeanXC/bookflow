package com.bookflow.backend.publicbooking.dto;

import com.bookflow.backend.staff.Staff;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Publicly bookable staff member")
public record PublicStaffResponse(
		Long id,
		String firstName,
		String lastName) {

	public static PublicStaffResponse from(Staff staff) {
		return new PublicStaffResponse(
				staff.getId(),
				staff.getFirstName(),
				staff.getLastName());
	}
}
