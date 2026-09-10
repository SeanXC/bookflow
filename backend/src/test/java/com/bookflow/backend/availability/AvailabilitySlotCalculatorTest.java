package com.bookflow.backend.availability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bookflow.backend.staff.Staff;
import com.bookflow.backend.tenant.Tenant;

class AvailabilitySlotCalculatorTest {

	private static final LocalDate MONDAY = LocalDate.of(2026, 9, 14);
	private static final LocalDate TUESDAY = LocalDate.of(2026, 9, 15);
	private static final ZoneId UTC = ZoneId.of("UTC");

	@Test
	void weeklyHoursProduceFifteenMinuteSlotsThatFitTheWindow() {
		List<AvailableSlot> slots = AvailabilitySlotCalculator.calculate(
				MONDAY,
				MONDAY,
				60,
				UTC,
				List.of(weeklyHours(DayOfWeek.MONDAY, "09:00", "11:00")),
				List.of(),
				List.of());

		assertEquals(List.of(
				slot("2026-09-14T09:00:00Z", "2026-09-14T10:00:00Z"),
				slot("2026-09-14T09:15:00Z", "2026-09-14T10:15:00Z"),
				slot("2026-09-14T09:30:00Z", "2026-09-14T10:30:00Z"),
				slot("2026-09-14T09:45:00Z", "2026-09-14T10:45:00Z"),
				slot("2026-09-14T10:00:00Z", "2026-09-14T11:00:00Z")), slots);
	}

	@Test
	void daysWithoutMatchingWeeklyHoursProduceNoSlots() {
		List<AvailableSlot> slots = AvailabilitySlotCalculator.calculate(
				MONDAY,
				TUESDAY,
				60,
				UTC,
				List.of(weeklyHours(DayOfWeek.MONDAY, "09:00", "10:00")),
				List.of(),
				List.of());

		assertEquals(
				List.of(slot("2026-09-14T09:00:00Z", "2026-09-14T10:00:00Z")),
				slots);
	}

	@Test
	void customHoursReplaceWeeklyHoursForThatDate() {
		List<AvailableSlot> slots = AvailabilitySlotCalculator.calculate(
				MONDAY,
				MONDAY,
				60,
				UTC,
				List.of(weeklyHours(DayOfWeek.MONDAY, "09:00", "17:00")),
				List.of(exception(
						MONDAY,
						AvailabilityExceptionType.CUSTOM_HOURS,
						"10:00",
						"11:00")),
				List.of());

		assertEquals(
				List.of(slot("2026-09-14T10:00:00Z", "2026-09-14T11:00:00Z")),
				slots);
	}

	@Test
	void allDayUnavailabilityClearsTheDate() {
		List<AvailableSlot> slots = AvailabilitySlotCalculator.calculate(
				MONDAY,
				MONDAY,
				30,
				UTC,
				List.of(weeklyHours(DayOfWeek.MONDAY, "09:00", "17:00")),
				List.of(new StaffAvailabilityException(
						tenant(),
						staff(),
						MONDAY,
						AvailabilityExceptionType.UNAVAILABLE,
						null,
						null,
						"Holiday")),
				List.of());

		assertEquals(List.of(), slots);
	}

	@Test
	void partialUnavailabilityTrimsOpenWindows() {
		List<AvailableSlot> slots = AvailabilitySlotCalculator.calculate(
				MONDAY,
				MONDAY,
				60,
				UTC,
				List.of(weeklyHours(DayOfWeek.MONDAY, "09:00", "12:00")),
				List.of(exception(
						MONDAY,
						AvailabilityExceptionType.UNAVAILABLE,
						"10:00",
						"11:00")),
				List.of());

		assertEquals(List.of(
				slot("2026-09-14T09:00:00Z", "2026-09-14T10:00:00Z"),
				slot("2026-09-14T11:00:00Z", "2026-09-14T12:00:00Z")), slots);
	}

	@Test
	void busyPeriodsRemoveOverlappingSlotsButKeepAdjacentTimes() {
		List<AvailableSlot> slots = AvailabilitySlotCalculator.calculate(
				MONDAY,
				MONDAY,
				60,
				UTC,
				List.of(weeklyHours(DayOfWeek.MONDAY, "09:00", "12:00")),
				List.of(),
				List.of(new BusyPeriod(
						Instant.parse("2026-09-14T10:00:00Z"),
						Instant.parse("2026-09-14T11:00:00Z"))));

		assertEquals(List.of(
				slot("2026-09-14T09:00:00Z", "2026-09-14T10:00:00Z"),
				slot("2026-09-14T11:00:00Z", "2026-09-14T12:00:00Z")), slots);
	}

	@Test
	void exceptionsOnAnotherDateDoNotChangeTheCurrentDay() {
		List<AvailableSlot> slots = AvailabilitySlotCalculator.calculate(
				MONDAY,
				MONDAY,
				60,
				UTC,
				List.of(weeklyHours(DayOfWeek.MONDAY, "09:00", "10:00")),
				List.of(exception(
						TUESDAY,
						AvailabilityExceptionType.UNAVAILABLE,
						"09:00",
						"17:00")),
				List.of());

		assertEquals(
				List.of(slot("2026-09-14T09:00:00Z", "2026-09-14T10:00:00Z")),
				slots);
	}

	@Test
	void slotsUseTheBusinessTimeZone() {
		List<AvailableSlot> slots = AvailabilitySlotCalculator.calculate(
				MONDAY,
				MONDAY,
				60,
				ZoneId.of("Europe/Dublin"),
				List.of(weeklyHours(DayOfWeek.MONDAY, "09:00", "10:00")),
				List.of(),
				List.of());

		assertEquals(
				List.of(slot("2026-09-14T08:00:00Z", "2026-09-14T09:00:00Z")),
				slots);
	}

	@Test
	void subtractWindowsRemovesTheCutAndKeepsTheRemainingSegments() {
		List<LocalTimeWindow> remaining = AvailabilitySlotCalculator.subtractWindows(
				List.of(new LocalTimeWindow(LocalTime.of(9, 0), LocalTime.of(17, 0))),
				List.of(new LocalTimeWindow(LocalTime.of(12, 0), LocalTime.of(13, 0))));

		assertEquals(List.of(
				new LocalTimeWindow(LocalTime.of(9, 0), LocalTime.of(12, 0)),
				new LocalTimeWindow(LocalTime.of(13, 0), LocalTime.of(17, 0))), remaining);
	}

	@Test
	void adjacentWindowsDoNotOverlap() {
		assertFalse(AvailabilitySlotCalculator.windowsOverlap(
				new LocalTimeWindow(LocalTime.of(9, 0), LocalTime.of(10, 0)),
				new LocalTimeWindow(LocalTime.of(10, 0), LocalTime.of(11, 0))));
		assertTrue(AvailabilitySlotCalculator.windowsOverlap(
				new LocalTimeWindow(LocalTime.of(9, 0), LocalTime.of(10, 30)),
				new LocalTimeWindow(LocalTime.of(10, 0), LocalTime.of(11, 0))));
	}

	private AvailableSlot slot(String start, String end) {
		return new AvailableSlot(Instant.parse(start), Instant.parse(end));
	}

	private StaffWeeklyHours weeklyHours(DayOfWeek dayOfWeek, String start, String end) {
		return new StaffWeeklyHours(
				tenant(),
				staff(),
				dayOfWeek,
				LocalTime.parse(start),
				LocalTime.parse(end));
	}

	private StaffAvailabilityException exception(
			LocalDate date,
			AvailabilityExceptionType type,
			String start,
			String end) {
		return new StaffAvailabilityException(
				tenant(),
				staff(),
				date,
				type,
				LocalTime.parse(start),
				LocalTime.parse(end),
				null);
	}

	private Tenant tenant() {
		return new Tenant("Glow Studio", "hello@example.com", null);
	}

	private Staff staff() {
		return new Staff(tenant(), null, "Anna", "Smith", null);
	}
}
