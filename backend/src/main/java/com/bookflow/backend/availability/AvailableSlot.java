package com.bookflow.backend.availability;

import java.time.Instant;

public record AvailableSlot(Instant startTime, Instant endTime) {
}
