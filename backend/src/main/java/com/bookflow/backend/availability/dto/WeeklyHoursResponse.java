package com.bookflow.backend.availability.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.bookflow.backend.availability.StaffWeeklyHours;

public record WeeklyHoursResponse(
		Long id,
		Long staffId,
		DayOfWeek dayOfWeek,
		LocalTime startTime,
		LocalTime endTime) {

	public static WeeklyHoursResponse from(StaffWeeklyHours hours) {
		return new WeeklyHoursResponse(
				hours.getId(),
				hours.getStaff().getId(),
				hours.getDayOfWeek(),
				hours.getStartTime(),
				hours.getEndTime());
	}
}
