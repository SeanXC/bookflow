package com.bookflow.backend.staff.dto;

import com.bookflow.backend.staff.Staff;

public record StaffResponse(
		Long id,
		Long userId,
		String firstName,
		String lastName,
		String phone,
		boolean active) {

	public static StaffResponse from(Staff staff) {
		Long userId = staff.getUser() == null ? null : staff.getUser().getId();
		return new StaffResponse(
				staff.getId(),
				userId,
				staff.getFirstName(),
				staff.getLastName(),
				staff.getPhone(),
				staff.isActive());
	}
}
