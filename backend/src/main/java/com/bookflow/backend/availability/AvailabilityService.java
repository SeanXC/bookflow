package com.bookflow.backend.availability;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookflow.backend.appointment.AppointmentRepository;
import com.bookflow.backend.appointment.AppointmentStatus;
import com.bookflow.backend.common.exception.DuplicateResourceException;
import com.bookflow.backend.common.exception.InvalidOperationException;
import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.security.CurrentUserProvider;
import com.bookflow.backend.service.ServiceRepository;
import com.bookflow.backend.staff.Staff;
import com.bookflow.backend.staff.StaffRepository;
import com.bookflow.backend.user.Role;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityService {

	private final StaffWeeklyHoursRepository weeklyHoursRepository;
	private final StaffAvailabilityExceptionRepository exceptionRepository;
	private final StaffRepository staffRepository;
	private final ServiceRepository serviceRepository;
	private final AppointmentRepository appointmentRepository;
	private final CurrentUserProvider currentUserProvider;
	private final Clock businessClock;

	@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST', 'STAFF')")
	public List<StaffWeeklyHours> getWeeklyHours(Long tenantId, Long staffId) {
		Staff staff = getAccessibleStaff(tenantId, staffId);
		return weeklyHoursRepository
				.findAllByTenantIdAndStaffIdOrderByDayOfWeekAscStartTimeAsc(
						tenantId,
						staff.getId());
	}

	@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST')")
	@Transactional
	public StaffWeeklyHours createWeeklyHours(
			Long tenantId,
			Long staffId,
			DayOfWeek dayOfWeek,
			LocalTime startTime,
			LocalTime endTime) {
		Staff staff = getStaff(tenantId, staffId);
		validateWeeklyOverlap(tenantId, staffId, dayOfWeek, startTime, endTime, null);
		try {
			return weeklyHoursRepository.saveAndFlush(new StaffWeeklyHours(
					staff.getTenant(),
					staff,
					dayOfWeek,
					startTime,
					endTime));
		} catch (DataIntegrityViolationException exception) {
			throw duplicateWeeklyHours();
		}
	}

	@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST')")
	@Transactional
	public StaffWeeklyHours updateWeeklyHours(
			Long tenantId,
			Long staffId,
			Long hoursId,
			DayOfWeek dayOfWeek,
			LocalTime startTime,
			LocalTime endTime) {
		StaffWeeklyHours hours = getWeeklyHoursRecord(tenantId, staffId, hoursId);
		validateWeeklyOverlap(tenantId, staffId, dayOfWeek, startTime, endTime, hoursId);
		hours.updateWindow(dayOfWeek, startTime, endTime);
		try {
			weeklyHoursRepository.flush();
			return hours;
		} catch (DataIntegrityViolationException exception) {
			throw duplicateWeeklyHours();
		}
	}

	@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST')")
	@Transactional
	public void deleteWeeklyHours(Long tenantId, Long staffId, Long hoursId) {
		weeklyHoursRepository.delete(getWeeklyHoursRecord(tenantId, staffId, hoursId));
	}

	@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST', 'STAFF')")
	public List<StaffAvailabilityException> getExceptions(
			Long tenantId,
			Long staffId,
			LocalDate fromDate,
			LocalDate toDate) {
		Staff staff = getAccessibleStaff(tenantId, staffId);
		validateDateRange(
				fromDate,
				toDate,
				AvailabilitySlotCalculator.MAX_EXCEPTION_RANGE_DAYS,
				"exception");
		return exceptionRepository
				.findAllByTenantIdAndStaffIdAndExceptionDateBetweenOrderByExceptionDateAscStartTimeAsc(
						tenantId,
						staff.getId(),
						fromDate,
						toDate);
	}

	@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST')")
	@Transactional
	public StaffAvailabilityException createException(
			Long tenantId,
			Long staffId,
			LocalDate exceptionDate,
			AvailabilityExceptionType type,
			LocalTime startTime,
			LocalTime endTime,
			String note) {
		Staff staff = getStaff(tenantId, staffId);
		validateExceptionOverlap(
				tenantId,
				staffId,
				exceptionDate,
				type,
				startTime,
				endTime,
				null);
		try {
			return exceptionRepository.saveAndFlush(new StaffAvailabilityException(
					staff.getTenant(),
					staff,
					exceptionDate,
					type,
					startTime,
					endTime,
					note));
		} catch (DataIntegrityViolationException exception) {
			throw duplicateException();
		}
	}

	@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST')")
	@Transactional
	public StaffAvailabilityException updateException(
			Long tenantId,
			Long staffId,
			Long exceptionId,
			LocalDate exceptionDate,
			AvailabilityExceptionType type,
			LocalTime startTime,
			LocalTime endTime,
			String note) {
		StaffAvailabilityException exception = getExceptionRecord(
				tenantId,
				staffId,
				exceptionId);
		validateExceptionOverlap(
				tenantId,
				staffId,
				exceptionDate,
				type,
				startTime,
				endTime,
				exceptionId);
		exception.updateDetails(exceptionDate, type, startTime, endTime, note);
		try {
			exceptionRepository.flush();
			return exception;
		} catch (DataIntegrityViolationException integrityException) {
			throw duplicateException();
		}
	}

	@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST')")
	@Transactional
	public void deleteException(Long tenantId, Long staffId, Long exceptionId) {
		exceptionRepository.delete(getExceptionRecord(tenantId, staffId, exceptionId));
	}

	@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST', 'STAFF')")
	public List<AvailableSlot> getAvailableSlots(
			Long tenantId,
			Long staffId,
			Long serviceId,
			LocalDate fromDate,
			LocalDate toDate) {
		Staff staff = getAccessibleStaff(tenantId, staffId);
		if (!staff.isActive()) {
			throw new InvalidOperationException("Inactive staff cannot be booked");
		}
		com.bookflow.backend.service.Service service = serviceRepository
				.findByIdAndTenantId(serviceId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Service", serviceId));
		if (!service.isActive()) {
			throw new InvalidOperationException("Inactive services cannot be booked");
		}
		return calculateSlots(tenantId, staff, service, fromDate, toDate);
	}

	public List<AvailableSlot> calculateSlots(
			Long tenantId,
			Staff staff,
			com.bookflow.backend.service.Service service,
			LocalDate fromDate,
			LocalDate toDate) {
		validateDateRange(
				fromDate,
				toDate,
				AvailabilitySlotCalculator.MAX_SLOT_RANGE_DAYS,
				"slot");

		ZoneId zone = businessClock.getZone();
		var rangeStart = fromDate.atStartOfDay(zone).toInstant();
		var rangeEnd = toDate.plusDays(1).atStartOfDay(zone).toInstant();
		List<StaffWeeklyHours> weeklyHours = weeklyHoursRepository
				.findAllByTenantIdAndStaffIdOrderByDayOfWeekAscStartTimeAsc(
						tenantId,
						staff.getId());
		List<StaffAvailabilityException> exceptions = exceptionRepository
				.findAllByTenantIdAndStaffIdAndExceptionDateBetweenOrderByExceptionDateAscStartTimeAsc(
						tenantId,
						staff.getId(),
						fromDate,
						toDate);
		List<BusyPeriod> busyPeriods = appointmentRepository
				.findAllByTenantIdAndStaffIdAndStatusNotAndEndTimeGreaterThanAndStartTimeLessThan(
						tenantId,
						staff.getId(),
						AppointmentStatus.CANCELLED,
						rangeStart,
						rangeEnd)
				.stream()
				.map(appointment -> new BusyPeriod(
						appointment.getStartTime(),
						appointment.getEndTime()))
				.toList();

		return AvailabilitySlotCalculator.calculate(
				fromDate,
				toDate,
				service.getDurationMinutes(),
				zone,
				weeklyHours,
				exceptions,
				busyPeriods);
	}

	@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST')")
	public void ensureRequestedSlotIsAvailable(
			Long tenantId,
			Long staffId,
			int durationMinutes,
			Instant startTime) {
		getStaff(tenantId, staffId);
		assertRequestedSlotIsAvailable(tenantId, staffId, durationMinutes, startTime);
	}

	public void assertRequestedSlotIsAvailable(
			Long tenantId,
			Long staffId,
			int durationMinutes,
			Instant startTime) {
		ZoneId zone = businessClock.getZone();
		LocalDate date = LocalDate.ofInstant(startTime, zone);
		boolean bookable = AvailabilitySlotCalculator.calculate(
				date,
				date,
				durationMinutes,
				zone,
				weeklyHoursRepository
						.findAllByTenantIdAndStaffIdOrderByDayOfWeekAscStartTimeAsc(
								tenantId,
								staffId),
				exceptionRepository
						.findAllByTenantIdAndStaffIdAndExceptionDateBetweenOrderByExceptionDateAscStartTimeAsc(
								tenantId,
								staffId,
								date,
								date),
				List.of())
				.stream()
				.anyMatch(slot -> slot.startTime().equals(startTime));
		if (!bookable) {
			throw new InvalidOperationException(
					"The requested start time is not an available booking slot");
		}
	}

	private Staff getAccessibleStaff(Long tenantId, Long staffId) {
		Staff staff = getStaff(tenantId, staffId);
		if (currentUserProvider.getRole() != Role.STAFF) {
			return staff;
		}
		if (staff.getUser() == null
				|| !staff.getUser().getId().equals(currentUserProvider.getUserId())) {
			throw new AccessDeniedException(
					"Staff users can only view their own availability");
		}
		return staff;
	}

	private Staff getStaff(Long tenantId, Long staffId) {
		return staffRepository.findByIdAndTenantId(staffId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));
	}

	private StaffWeeklyHours getWeeklyHoursRecord(
			Long tenantId,
			Long staffId,
			Long hoursId) {
		StaffWeeklyHours hours = weeklyHoursRepository.findByIdAndTenantId(hoursId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Weekly hours", hoursId));
		if (!hours.getStaff().getId().equals(staffId)) {
			throw new ResourceNotFoundException("Weekly hours", hoursId);
		}
		return hours;
	}

	private StaffAvailabilityException getExceptionRecord(
			Long tenantId,
			Long staffId,
			Long exceptionId) {
		StaffAvailabilityException exception = exceptionRepository
				.findByIdAndTenantId(exceptionId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Availability exception",
						exceptionId));
		if (!exception.getStaff().getId().equals(staffId)) {
			throw new ResourceNotFoundException("Availability exception", exceptionId);
		}
		return exception;
	}

	private void validateWeeklyOverlap(
			Long tenantId,
			Long staffId,
			DayOfWeek dayOfWeek,
			LocalTime startTime,
			LocalTime endTime,
			Long currentId) {
		LocalTimeWindow incoming = new LocalTimeWindow(startTime, endTime);
		boolean overlaps = weeklyHoursRepository
				.findAllByTenantIdAndStaffIdAndDayOfWeek(tenantId, staffId, dayOfWeek)
				.stream()
				.filter(hours -> currentId == null || !hours.getId().equals(currentId))
				.anyMatch(hours -> incoming.overlaps(new LocalTimeWindow(
						hours.getStartTime(),
						hours.getEndTime())));
		if (overlaps) {
			throw new InvalidOperationException(
					"Weekly hours cannot overlap on the same day");
		}
	}

	private void validateExceptionOverlap(
			Long tenantId,
			Long staffId,
			LocalDate exceptionDate,
			AvailabilityExceptionType type,
			LocalTime startTime,
			LocalTime endTime,
			Long currentId) {
		List<StaffAvailabilityException> existing = exceptionRepository
				.findAllByTenantIdAndStaffIdAndExceptionDate(
						tenantId,
						staffId,
						exceptionDate)
				.stream()
				.filter(item -> currentId == null || !item.getId().equals(currentId))
				.toList();
		boolean incomingAllDay = type == AvailabilityExceptionType.UNAVAILABLE
				&& startTime == null;
		boolean existingAllDay = existing.stream().anyMatch(item ->
				item.getType() == AvailabilityExceptionType.UNAVAILABLE
						&& item.getStartTime() == null);
		if (incomingAllDay && !existing.isEmpty()) {
			throw new InvalidOperationException(
					"An all-day unavailability cannot share a date with other exceptions");
		}
		if (existingAllDay) {
			throw new InvalidOperationException(
					"This date already has an all-day unavailability");
		}
		if (startTime == null || endTime == null) {
			return;
		}

		LocalTimeWindow incoming = new LocalTimeWindow(startTime, endTime);
		boolean overlaps = existing.stream()
				.filter(item -> item.getStartTime() != null)
				.anyMatch(item -> incoming.overlaps(new LocalTimeWindow(
						item.getStartTime(),
						item.getEndTime())));
		if (overlaps) {
			throw new InvalidOperationException(
					"Availability exceptions cannot overlap on the same date");
		}
	}

	private void validateDateRange(
			LocalDate fromDate,
			LocalDate toDate,
			int maxDays,
			String rangeName) {
		if (fromDate.isAfter(toDate)) {
			throw new InvalidOperationException(
					"The %s start date must not be after the end date".formatted(rangeName));
		}
		long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
		if (days > maxDays) {
			throw new InvalidOperationException(
					"The %s date range cannot exceed %d days".formatted(rangeName, maxDays));
		}
	}

	private DuplicateResourceException duplicateWeeklyHours() {
		return new DuplicateResourceException(
				"This weekly hours window already exists");
	}

	private DuplicateResourceException duplicateException() {
		return new DuplicateResourceException(
				"This availability exception already exists");
	}
}
