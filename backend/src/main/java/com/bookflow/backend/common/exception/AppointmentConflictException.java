package com.bookflow.backend.common.exception;

public class AppointmentConflictException extends RuntimeException {

	public AppointmentConflictException() {
		super("This staff member already has an appointment during the selected time.");
	}
}
