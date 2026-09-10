package com.bookflow.backend.availability.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.bookflow.backend.availability.AvailabilityExceptionType;
import com.bookflow.backend.availability.StaffAvailabilityException;

public record AvailabilityExceptionResponse(
		Long id,
		Long staffId,
		LocalDate exceptionDate,
		AvailabilityExceptionType type,
		LocalTime startTime,
		LocalTime endTime,
		String note) {

	public static AvailabilityExceptionResponse from(StaffAvailabilityException exception) {
		return new AvailabilityExceptionResponse(
				exception.getId(),
				exception.getStaff().getId(),
				exception.getExceptionDate(),
				exception.getType(),
				exception.getStartTime(),
				exception.getEndTime(),
				exception.getNote());
	}
}
