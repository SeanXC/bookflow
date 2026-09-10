package com.bookflow.backend.availability;

import java.time.Instant;

public record BusyPeriod(Instant start, Instant end) {

	boolean overlaps(Instant slotStart, Instant slotEnd) {
		return start.isBefore(slotEnd) && slotStart.isBefore(end);
	}
}
