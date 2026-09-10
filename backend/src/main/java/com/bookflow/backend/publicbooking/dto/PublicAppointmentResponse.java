package com.bookflow.backend.publicbooking.dto;

import java.time.Instant;

import com.bookflow.backend.appointment.Appointment;
import com.bookflow.backend.appointment.AppointmentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Public booking confirmation")
public record PublicAppointmentResponse(
		Long id,
		PublicStaffResponse staff,
		PublicServiceResponse service,
		String customerFirstName,
		String customerLastName,
		Instant startTime,
		Instant endTime,
		AppointmentStatus status,
		String notes) {

	public static PublicAppointmentResponse from(Appointment appointment) {
		return new PublicAppointmentResponse(
				appointment.getId(),
				PublicStaffResponse.from(appointment.getStaff()),
				PublicServiceResponse.from(appointment.getService()),
				appointment.getCustomer().getFirstName(),
				appointment.getCustomer().getLastName(),
				appointment.getStartTime(),
				appointment.getEndTime(),
				appointment.getStatus(),
				appointment.getNotes());
	}
}
