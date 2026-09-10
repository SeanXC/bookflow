package com.bookflow.backend.availability;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AvailabilitySlotCalculator {

	public static final int SLOT_STEP_MINUTES = 15;
	public static final int MAX_SLOT_RANGE_DAYS = 14;
	public static final int MAX_EXCEPTION_RANGE_DAYS = 31;

	private AvailabilitySlotCalculator() {
	}

	public static List<AvailableSlot> calculate(
			LocalDate fromDate,
			LocalDate toDate,
			int durationMinutes,
			ZoneId zone,
			List<StaffWeeklyHours> weeklyHours,
			List<StaffAvailabilityException> exceptions,
			List<BusyPeriod> busyPeriods) {
		List<AvailableSlot> slots = new ArrayList<>();
		for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
			slots.addAll(calculateForDate(
					date,
					durationMinutes,
					zone,
					weeklyHours,
					exceptions,
					busyPeriods));
		}
		return List.copyOf(slots);
	}

	static List<AvailableSlot> calculateForDate(
			LocalDate date,
			int durationMinutes,
			ZoneId zone,
			List<StaffWeeklyHours> weeklyHours,
			List<StaffAvailabilityException> exceptions,
			List<BusyPeriod> busyPeriods) {
		List<StaffAvailabilityException> dayExceptions = exceptions.stream()
				.filter(exception -> exception.getExceptionDate().equals(date))
				.toList();
		if (dayExceptions.stream().anyMatch(AvailabilitySlotCalculator::isAllDayUnavailable)) {
			return List.of();
		}

		List<LocalTimeWindow> windows = workingWindows(date, weeklyHours, dayExceptions);
		List<LocalTimeWindow> unavailable = dayExceptions.stream()
				.filter(exception -> exception.getType() == AvailabilityExceptionType.UNAVAILABLE)
				.filter(exception -> exception.getStartTime() != null)
				.map(exception -> new LocalTimeWindow(
						exception.getStartTime(),
						exception.getEndTime()))
				.toList();
		List<LocalTimeWindow> openWindows = subtractWindows(windows, unavailable);
		Duration duration = Duration.ofMinutes(durationMinutes);
		Duration step = Duration.ofMinutes(SLOT_STEP_MINUTES);
		List<AvailableSlot> slots = new ArrayList<>();

		for (LocalTimeWindow window : openWindows) {
			var cursor = date.atTime(window.start()).atZone(zone).toInstant();
			var windowEnd = date.atTime(window.end()).atZone(zone).toInstant();
			while (!cursor.plus(duration).isAfter(windowEnd)) {
				var slotEnd = cursor.plus(duration);
				Instant slotStart = cursor;
				boolean busy = busyPeriods.stream()
						.anyMatch(period -> period.overlaps(slotStart, slotEnd));
				if (!busy) {
					slots.add(new AvailableSlot(slotStart, slotEnd));
				}
				cursor = cursor.plus(step);
			}
		}
		return slots;
	}

	static boolean windowsOverlap(LocalTimeWindow left, LocalTimeWindow right) {
		return left.overlaps(right);
	}

	private static boolean isAllDayUnavailable(StaffAvailabilityException exception) {
		return exception.getType() == AvailabilityExceptionType.UNAVAILABLE
				&& exception.getStartTime() == null;
	}

	private static List<LocalTimeWindow> workingWindows(
			LocalDate date,
			List<StaffWeeklyHours> weeklyHours,
			List<StaffAvailabilityException> dayExceptions) {
		List<LocalTimeWindow> customHours = dayExceptions.stream()
				.filter(exception -> exception.getType() == AvailabilityExceptionType.CUSTOM_HOURS)
				.map(exception -> new LocalTimeWindow(
						exception.getStartTime(),
						exception.getEndTime()))
				.sorted(Comparator.comparing(LocalTimeWindow::start))
				.toList();
		if (!customHours.isEmpty()) {
			return customHours;
		}
		return weeklyHours.stream()
				.filter(hours -> hours.getDayOfWeek() == date.getDayOfWeek())
				.map(hours -> new LocalTimeWindow(hours.getStartTime(), hours.getEndTime()))
				.sorted(Comparator.comparing(LocalTimeWindow::start))
				.toList();
	}

	static List<LocalTimeWindow> subtractWindows(
			List<LocalTimeWindow> windows,
			List<LocalTimeWindow> cuts) {
		List<LocalTimeWindow> current = new ArrayList<>(windows);
		for (LocalTimeWindow cut : cuts) {
			List<LocalTimeWindow> next = new ArrayList<>();
			for (LocalTimeWindow window : current) {
				next.addAll(subtractOne(window, cut));
			}
			current = next;
		}
		return current;
	}

	private static List<LocalTimeWindow> subtractOne(
			LocalTimeWindow window,
			LocalTimeWindow cut) {
		if (!window.overlaps(cut)) {
			return List.of(window);
		}

		List<LocalTimeWindow> remaining = new ArrayList<>();
		if (window.start().isBefore(cut.start())) {
			remaining.add(new LocalTimeWindow(window.start(), min(window.end(), cut.start())));
		}
		if (window.end().isAfter(cut.end())) {
			remaining.add(new LocalTimeWindow(max(window.start(), cut.end()), window.end()));
		}
		return remaining.stream()
				.filter(item -> item.end().isAfter(item.start()))
				.toList();
	}

	private static LocalTime min(LocalTime left, LocalTime right) {
		return left.isBefore(right) ? left : right;
	}

	private static LocalTime max(LocalTime left, LocalTime right) {
		return left.isAfter(right) ? left : right;
	}
}
