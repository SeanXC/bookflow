package com.bookflow.backend.availability;

import java.time.LocalTime;

record LocalTimeWindow(LocalTime start, LocalTime end) {

	boolean overlaps(LocalTimeWindow other) {
		return start.isBefore(other.end) && other.start.isBefore(end);
	}
}
