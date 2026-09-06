package com.bookflow.backend.dashboard;

import java.time.LocalDate;

public interface DailyBookingView {

	LocalDate getPeriod();

	long getBookings();
}
