package com.bookflow.backend.availability.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Recurring weekly working window")
public record WeeklyHoursRequest(
		@NotNull DayOfWeek dayOfWeek,
		@NotNull LocalTime startTime,
		@NotNull LocalTime endTime) {

	@AssertTrue(message = "end time must be after start time")
	public boolean isTimeRangeValid() {
		return startTime == null || endTime == null || endTime.isAfter(startTime);
	}
}
