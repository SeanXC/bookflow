package com.bookflow.backend.availability.dto;

import java.time.Instant;

import com.bookflow.backend.availability.AvailableSlot;

import io.swagger.v3.oas.annotations.media.Schema;

public record AvailableSlotResponse(
		@Schema(example = "2026-09-14T09:00:00Z")
		Instant startTime,
		@Schema(example = "2026-09-14T10:00:00Z")
		Instant endTime) {

	public static AvailableSlotResponse from(AvailableSlot slot) {
		return new AvailableSlotResponse(slot.startTime(), slot.endTime());
	}
}
