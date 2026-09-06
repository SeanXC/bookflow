package com.bookflow.backend.appointment.dto;

import com.bookflow.backend.staff.Staff;

public record StaffSummary(
		Long id,
		String firstName,
		String lastName) {

	public static StaffSummary from(Staff staff) {
		return new StaffSummary(
				staff.getId(),
				staff.getFirstName(),
				staff.getLastName());
	}
}
