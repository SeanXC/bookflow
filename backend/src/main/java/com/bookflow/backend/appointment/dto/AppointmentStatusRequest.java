package com.bookflow.backend.appointment.dto;

import com.bookflow.backend.appointment.AppointmentStatus;

import jakarta.validation.constraints.NotNull;

public record AppointmentStatusRequest(
		@NotNull AppointmentStatus status) {
}
