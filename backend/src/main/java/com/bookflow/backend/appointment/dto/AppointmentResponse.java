package com.bookflow.backend.appointment.dto;

import java.time.Instant;

import com.bookflow.backend.appointment.Appointment;
import com.bookflow.backend.appointment.AppointmentStatus;

public record AppointmentResponse(
		Long id,
		Long customerId,
		Long staffId,
		Long serviceId,
		CustomerSummary customer,
		StaffSummary staff,
		ServiceSummary service,
		Instant startTime,
		Instant endTime,
		AppointmentStatus status,
		String notes,
		Instant createdAt,
		Instant updatedAt) {

	public static AppointmentResponse from(Appointment appointment) {
		return new AppointmentResponse(
				appointment.getId(),
				appointment.getCustomer().getId(),
				appointment.getStaff().getId(),
				appointment.getService().getId(),
				CustomerSummary.from(appointment.getCustomer()),
				StaffSummary.from(appointment.getStaff()),
				ServiceSummary.from(appointment.getService()),
				appointment.getStartTime(),
				appointment.getEndTime(),
				appointment.getStatus(),
				appointment.getNotes(),
				appointment.getCreatedAt(),
				appointment.getUpdatedAt());
	}
}
