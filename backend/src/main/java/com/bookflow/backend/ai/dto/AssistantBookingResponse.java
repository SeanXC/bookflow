package com.bookflow.backend.ai.dto;

import java.time.Instant;

import com.bookflow.backend.appointment.Appointment;
import com.bookflow.backend.appointment.AppointmentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "An appointment created after the guest confirmed a proposal")
public record AssistantBookingResponse(
		Long appointmentId,
		Long staffId,
		String staffFirstName,
		String staffLastName,
		Long serviceId,
		String serviceName,
		Instant startTime,
		Instant endTime,
		AppointmentStatus status,
		String firstName,
		String lastName) {

	public static AssistantBookingResponse from(Appointment appointment) {
		return new AssistantBookingResponse(
				appointment.getId(),
				appointment.getStaff().getId(),
				appointment.getStaff().getFirstName(),
				appointment.getStaff().getLastName(),
				appointment.getService().getId(),
				appointment.getService().getName(),
				appointment.getStartTime(),
				appointment.getEndTime(),
				appointment.getStatus(),
				appointment.getCustomer().getFirstName(),
				appointment.getCustomer().getLastName());
	}
}
