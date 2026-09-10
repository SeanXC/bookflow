package com.bookflow.backend.availability.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.bookflow.backend.availability.AvailabilityExceptionType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Date-specific availability exception")
public record AvailabilityExceptionRequest(
		@NotNull LocalDate exceptionDate,
		@NotNull AvailabilityExceptionType type,
		LocalTime startTime,
		LocalTime endTime,
		@Size(max = 255) String note) {

	@AssertTrue(message = "exception time range must be complete and ordered")
	public boolean isTimeRangeValid() {
		if (startTime == null && endTime == null) {
			return type != AvailabilityExceptionType.CUSTOM_HOURS;
		}
		return startTime != null && endTime != null && endTime.isAfter(startTime);
	}
}
