package com.bookflow.backend.availability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import com.bookflow.backend.appointment.Appointment;
import com.bookflow.backend.appointment.AppointmentRepository;
import com.bookflow.backend.common.exception.DuplicateResourceException;
import com.bookflow.backend.common.exception.InvalidOperationException;
import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.security.CurrentUserProvider;
import com.bookflow.backend.service.ServiceRepository;
import com.bookflow.backend.staff.Staff;
import com.bookflow.backend.staff.StaffRepository;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.user.Role;
import com.bookflow.backend.user.User;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

	private static final Long TENANT_ID = 10L;
	private static final Long STAFF_ID = 20L;
	private static final Long HOURS_ID = 30L;
	private static final Long EXCEPTION_ID = 40L;
	private static final Long SERVICE_ID = 50L;
	private static final Long USER_ID = 60L;
	private static final LocalDate MONDAY = LocalDate.of(2026, 9, 14);
	private static final LocalTime NINE = LocalTime.of(9, 0);
	private static final LocalTime NOON = LocalTime.of(12, 0);

	@Mock
	private StaffWeeklyHoursRepository weeklyHoursRepository;

	@Mock
	private StaffAvailabilityExceptionRepository exceptionRepository;

	@Mock
	private StaffRepository staffRepository;

	@Mock
	private ServiceRepository serviceRepository;

	@Mock
	private AppointmentRepository appointmentRepository;

	@Mock
	private CurrentUserProvider currentUserProvider;

	private AvailabilityService availabilityService;

	@BeforeEach
	void setUp() {
		availabilityService = new AvailabilityService(
				weeklyHoursRepository,
				exceptionRepository,
				staffRepository,
				serviceRepository,
				appointmentRepository,
				currentUserProvider,
				Clock.fixed(Instant.parse("2026-09-12T10:00:00Z"), ZoneOffset.UTC));
	}

	@Test
	void ownerCanReadWeeklyHours() {
		Staff staff = staff(true);
		StaffWeeklyHours hours = weeklyHours(staff, HOURS_ID, NINE, NOON);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(currentUserProvider.getRole()).thenReturn(Role.OWNER);
		when(weeklyHoursRepository
				.findAllByTenantIdAndStaffIdOrderByDayOfWeekAscStartTimeAsc(
						TENANT_ID,
						STAFF_ID))
				.thenReturn(List.of(hours));

		List<StaffWeeklyHours> result =
				availabilityService.getWeeklyHours(TENANT_ID, STAFF_ID);

		assertEquals(List.of(hours), result);
	}

	@Test
	void staffCanReadOnlyTheirOwnAvailability() {
		Staff ownStaff = staff(true);
		User user = mock(User.class);
		when(ownStaff.getUser()).thenReturn(user);
		when(user.getId()).thenReturn(USER_ID);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(ownStaff));
		when(currentUserProvider.getRole()).thenReturn(Role.STAFF);
		when(currentUserProvider.getUserId()).thenReturn(USER_ID);
		when(weeklyHoursRepository
				.findAllByTenantIdAndStaffIdOrderByDayOfWeekAscStartTimeAsc(
						TENANT_ID,
						STAFF_ID))
				.thenReturn(List.of());

		assertTrue(availabilityService.getWeeklyHours(TENANT_ID, STAFF_ID).isEmpty());
	}

	@Test
	void staffCannotReadAnotherStaffMembersAvailability() {
		Staff otherStaff = staff(true);
		when(otherStaff.getUser()).thenReturn(null);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(otherStaff));
		when(currentUserProvider.getRole()).thenReturn(Role.STAFF);

		assertThrows(
				AccessDeniedException.class,
				() -> availabilityService.getWeeklyHours(TENANT_ID, STAFF_ID));
	}

	@Test
	void createWeeklyHoursSavesAValidatedWindow() {
		Staff staff = staff(true);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(weeklyHoursRepository.findAllByTenantIdAndStaffIdAndDayOfWeek(
				TENANT_ID,
				STAFF_ID,
				DayOfWeek.MONDAY))
				.thenReturn(List.of());
		when(weeklyHoursRepository.saveAndFlush(any(StaffWeeklyHours.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		StaffWeeklyHours result = availabilityService.createWeeklyHours(
				TENANT_ID,
				STAFF_ID,
				DayOfWeek.MONDAY,
				NINE,
				NOON);

		assertSame(staff, result.getStaff());
		assertEquals(DayOfWeek.MONDAY, result.getDayOfWeek());
		assertEquals(NINE, result.getStartTime());
		assertEquals(NOON, result.getEndTime());
	}

	@Test
	void createWeeklyHoursRejectsAnOverlappingWindow() {
		Staff staff = staff(true);
		StaffWeeklyHours existing =
				weeklyHours(staff, HOURS_ID, NINE, NOON);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(weeklyHoursRepository.findAllByTenantIdAndStaffIdAndDayOfWeek(
				TENANT_ID,
				STAFF_ID,
				DayOfWeek.MONDAY))
				.thenReturn(List.of(existing));

		assertThrows(
				InvalidOperationException.class,
				() -> availabilityService.createWeeklyHours(
						TENANT_ID,
						STAFF_ID,
						DayOfWeek.MONDAY,
						LocalTime.of(11, 0),
						LocalTime.of(13, 0)));
		verify(weeklyHoursRepository, never()).saveAndFlush(any());
	}

	@Test
	void createWeeklyHoursTranslatesDatabaseDuplicates() {
		Staff staff = staff(true);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(weeklyHoursRepository.findAllByTenantIdAndStaffIdAndDayOfWeek(
				TENANT_ID,
				STAFF_ID,
				DayOfWeek.MONDAY))
				.thenReturn(List.of());
		when(weeklyHoursRepository.saveAndFlush(any(StaffWeeklyHours.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate"));

		assertThrows(
				DuplicateResourceException.class,
				() -> availabilityService.createWeeklyHours(
						TENANT_ID,
						STAFF_ID,
						DayOfWeek.MONDAY,
						NINE,
						NOON));
	}

	@Test
	void updateWeeklyHoursExcludesTheCurrentRecordFromOverlapChecks() {
		Staff staff = staff(true);
		StaffWeeklyHours hours = weeklyHours(staff, HOURS_ID, NINE, NOON);
		when(weeklyHoursRepository.findByIdAndTenantId(HOURS_ID, TENANT_ID))
				.thenReturn(Optional.of(hours));
		when(weeklyHoursRepository.findAllByTenantIdAndStaffIdAndDayOfWeek(
				TENANT_ID,
				STAFF_ID,
				DayOfWeek.MONDAY))
				.thenReturn(List.of(hours));

		StaffWeeklyHours result = availabilityService.updateWeeklyHours(
				TENANT_ID,
				STAFF_ID,
				HOURS_ID,
				DayOfWeek.MONDAY,
				LocalTime.of(10, 0),
				LocalTime.of(13, 0));

		assertSame(hours, result);
		verify(hours).updateWindow(
				DayOfWeek.MONDAY,
				LocalTime.of(10, 0),
				LocalTime.of(13, 0));
		verify(weeklyHoursRepository).flush();
	}

	@Test
	void updateWeeklyHoursTranslatesDatabaseDuplicates() {
		Staff staff = staff(true);
		StaffWeeklyHours hours = weeklyHours(staff, HOURS_ID, NINE, NOON);
		when(weeklyHoursRepository.findByIdAndTenantId(HOURS_ID, TENANT_ID))
				.thenReturn(Optional.of(hours));
		when(weeklyHoursRepository.findAllByTenantIdAndStaffIdAndDayOfWeek(
				TENANT_ID,
				STAFF_ID,
				DayOfWeek.MONDAY))
				.thenReturn(List.of(hours));
		org.mockito.Mockito.doThrow(new DataIntegrityViolationException("duplicate"))
				.when(weeklyHoursRepository)
				.flush();

		assertThrows(
				DuplicateResourceException.class,
				() -> availabilityService.updateWeeklyHours(
						TENANT_ID,
						STAFF_ID,
						HOURS_ID,
						DayOfWeek.MONDAY,
						NINE,
						NOON));
	}

	@Test
	void weeklyHoursRecordsAreScopedToTheirStaffMember() {
		Staff otherStaff = staff(true);
		when(otherStaff.getId()).thenReturn(99L);
		StaffWeeklyHours hours = weeklyHours(otherStaff, HOURS_ID, NINE, NOON);
		when(weeklyHoursRepository.findByIdAndTenantId(HOURS_ID, TENANT_ID))
				.thenReturn(Optional.of(hours));

		assertThrows(
				ResourceNotFoundException.class,
				() -> availabilityService.deleteWeeklyHours(
						TENANT_ID,
						STAFF_ID,
						HOURS_ID));
		verify(weeklyHoursRepository, never()).delete(any());
	}

	@Test
	void deleteWeeklyHoursDeletesTheTenantScopedRecord() {
		Staff staff = staff(true);
		StaffWeeklyHours hours = weeklyHours(staff, HOURS_ID, NINE, NOON);
		when(weeklyHoursRepository.findByIdAndTenantId(HOURS_ID, TENANT_ID))
				.thenReturn(Optional.of(hours));

		availabilityService.deleteWeeklyHours(TENANT_ID, STAFF_ID, HOURS_ID);

		verify(weeklyHoursRepository).delete(hours);
	}

	@Test
	void getExceptionsValidatesAndQueriesTheRequestedRange() {
		Staff staff = staff(true);
		StaffAvailabilityException exception =
				availabilityException(staff, EXCEPTION_ID, MONDAY, NINE, NOON);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(currentUserProvider.getRole()).thenReturn(Role.OWNER);
		when(exceptionRepository
				.findAllByTenantIdAndStaffIdAndExceptionDateBetweenOrderByExceptionDateAscStartTimeAsc(
						TENANT_ID,
						STAFF_ID,
						MONDAY,
						MONDAY.plusDays(2)))
				.thenReturn(List.of(exception));

		List<StaffAvailabilityException> result = availabilityService.getExceptions(
				TENANT_ID,
				STAFF_ID,
				MONDAY,
				MONDAY.plusDays(2));

		assertEquals(List.of(exception), result);
	}

	@Test
	void exceptionRangesRejectReverseAndOversizedDates() {
		Staff staff = staff(true);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(currentUserProvider.getRole()).thenReturn(Role.OWNER);

		assertThrows(
				InvalidOperationException.class,
				() -> availabilityService.getExceptions(
						TENANT_ID,
						STAFF_ID,
						MONDAY.plusDays(1),
						MONDAY));
		assertThrows(
				InvalidOperationException.class,
				() -> availabilityService.getExceptions(
						TENANT_ID,
						STAFF_ID,
						MONDAY,
						MONDAY.plusDays(AvailabilitySlotCalculator.MAX_EXCEPTION_RANGE_DAYS)));
	}

	@Test
	void createExceptionSavesAValidatedWindow() {
		Staff staff = staff(true);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(exceptionRepository.findAllByTenantIdAndStaffIdAndExceptionDate(
				TENANT_ID,
				STAFF_ID,
				MONDAY))
				.thenReturn(List.of());
		when(exceptionRepository.saveAndFlush(any(StaffAvailabilityException.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		StaffAvailabilityException result = availabilityService.createException(
				TENANT_ID,
				STAFF_ID,
				MONDAY,
				AvailabilityExceptionType.CUSTOM_HOURS,
				NINE,
				NOON,
				"Short day");

		assertSame(staff, result.getStaff());
		assertEquals(MONDAY, result.getExceptionDate());
		assertEquals("Short day", result.getNote());
	}

	@Test
	void allDayExceptionCannotShareADateWithAnotherException() {
		Staff staff = staff(true);
		StaffAvailabilityException existing =
				availabilityException(staff, EXCEPTION_ID, MONDAY, NINE, NOON);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(exceptionRepository.findAllByTenantIdAndStaffIdAndExceptionDate(
				TENANT_ID,
				STAFF_ID,
				MONDAY))
				.thenReturn(List.of(existing));

		assertThrows(
				InvalidOperationException.class,
				() -> availabilityService.createException(
						TENANT_ID,
						STAFF_ID,
						MONDAY,
						AvailabilityExceptionType.UNAVAILABLE,
						null,
						null,
						"Closed"));
	}

	@Test
	void existingAllDayExceptionRejectsAnotherWindow() {
		Staff staff = staff(true);
		StaffAvailabilityException existing = availabilityException(
				staff,
				EXCEPTION_ID,
				MONDAY,
				null,
				null);
		when(existing.getType()).thenReturn(AvailabilityExceptionType.UNAVAILABLE);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(exceptionRepository.findAllByTenantIdAndStaffIdAndExceptionDate(
				TENANT_ID,
				STAFF_ID,
				MONDAY))
				.thenReturn(List.of(existing));

		assertThrows(
				InvalidOperationException.class,
				() -> availabilityService.createException(
						TENANT_ID,
						STAFF_ID,
						MONDAY,
						AvailabilityExceptionType.CUSTOM_HOURS,
						NINE,
						NOON,
						null));
	}

	@Test
	void timedExceptionsCannotOverlap() {
		Staff staff = staff(true);
		StaffAvailabilityException existing =
				availabilityException(staff, EXCEPTION_ID, MONDAY, NINE, NOON);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(exceptionRepository.findAllByTenantIdAndStaffIdAndExceptionDate(
				TENANT_ID,
				STAFF_ID,
				MONDAY))
				.thenReturn(List.of(existing));

		assertThrows(
				InvalidOperationException.class,
				() -> availabilityService.createException(
						TENANT_ID,
						STAFF_ID,
						MONDAY,
						AvailabilityExceptionType.CUSTOM_HOURS,
						LocalTime.of(11, 0),
						LocalTime.of(13, 0),
						null));
	}

	@Test
	void createExceptionTranslatesDatabaseDuplicates() {
		Staff staff = staff(true);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(exceptionRepository.findAllByTenantIdAndStaffIdAndExceptionDate(
				TENANT_ID,
				STAFF_ID,
				MONDAY))
				.thenReturn(List.of());
		when(exceptionRepository.saveAndFlush(any(StaffAvailabilityException.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate"));

		assertThrows(
				DuplicateResourceException.class,
				() -> availabilityService.createException(
						TENANT_ID,
						STAFF_ID,
						MONDAY,
						AvailabilityExceptionType.UNAVAILABLE,
						NINE,
						NOON,
						null));
	}

	@Test
	void updateExceptionExcludesTheCurrentRecordAndFlushes() {
		Staff staff = staff(true);
		StaffAvailabilityException exception =
				availabilityException(staff, EXCEPTION_ID, MONDAY, NINE, NOON);
		when(exceptionRepository.findByIdAndTenantId(EXCEPTION_ID, TENANT_ID))
				.thenReturn(Optional.of(exception));
		when(exceptionRepository.findAllByTenantIdAndStaffIdAndExceptionDate(
				TENANT_ID,
				STAFF_ID,
				MONDAY))
				.thenReturn(List.of(exception));

		StaffAvailabilityException result = availabilityService.updateException(
				TENANT_ID,
				STAFF_ID,
				EXCEPTION_ID,
				MONDAY,
				AvailabilityExceptionType.CUSTOM_HOURS,
				LocalTime.of(10, 0),
				LocalTime.of(13, 0),
				"Updated");

		assertSame(exception, result);
		verify(exception).updateDetails(
				MONDAY,
				AvailabilityExceptionType.CUSTOM_HOURS,
				LocalTime.of(10, 0),
				LocalTime.of(13, 0),
				"Updated");
		verify(exceptionRepository).flush();
	}

	@Test
	void exceptionRecordsAreScopedToTheirStaffMember() {
		Staff otherStaff = staff(true);
		when(otherStaff.getId()).thenReturn(99L);
		StaffAvailabilityException exception =
				availabilityException(otherStaff, EXCEPTION_ID, MONDAY, NINE, NOON);
		when(exceptionRepository.findByIdAndTenantId(EXCEPTION_ID, TENANT_ID))
				.thenReturn(Optional.of(exception));

		assertThrows(
				ResourceNotFoundException.class,
				() -> availabilityService.deleteException(
						TENANT_ID,
						STAFF_ID,
						EXCEPTION_ID));
		verify(exceptionRepository, never()).delete(any());
	}

	@Test
	void deleteExceptionDeletesTheTenantScopedRecord() {
		Staff staff = staff(true);
		StaffAvailabilityException exception =
				availabilityException(staff, EXCEPTION_ID, MONDAY, NINE, NOON);
		when(exceptionRepository.findByIdAndTenantId(EXCEPTION_ID, TENANT_ID))
				.thenReturn(Optional.of(exception));

		availabilityService.deleteException(TENANT_ID, STAFF_ID, EXCEPTION_ID);

		verify(exceptionRepository).delete(exception);
	}

	@Test
	void getAvailableSlotsRejectsInactiveOrUnknownResources() {
		Staff inactiveStaff = staff(false);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(inactiveStaff));
		when(currentUserProvider.getRole()).thenReturn(Role.OWNER);

		assertThrows(
				InvalidOperationException.class,
				() -> availabilityService.getAvailableSlots(
						TENANT_ID,
						STAFF_ID,
						SERVICE_ID,
						MONDAY,
						MONDAY));

		Staff activeStaff = staff(true);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(activeStaff));
		when(serviceRepository.findByIdAndTenantId(SERVICE_ID, TENANT_ID))
				.thenReturn(Optional.empty());

		assertThrows(
				ResourceNotFoundException.class,
				() -> availabilityService.getAvailableSlots(
						TENANT_ID,
						STAFF_ID,
						SERVICE_ID,
						MONDAY,
						MONDAY));
	}

	@Test
	void getAvailableSlotsRejectsAnInactiveService() {
		Staff staff = staff(true);
		com.bookflow.backend.service.Service service = service();
		service.deactivate();
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(currentUserProvider.getRole()).thenReturn(Role.OWNER);
		when(serviceRepository.findByIdAndTenantId(SERVICE_ID, TENANT_ID))
				.thenReturn(Optional.of(service));

		assertThrows(
				InvalidOperationException.class,
				() -> availabilityService.getAvailableSlots(
						TENANT_ID,
						STAFF_ID,
						SERVICE_ID,
						MONDAY,
						MONDAY));
	}

	@Test
	void getAvailableSlotsCalculatesAroundExistingAppointments() {
		Staff staff = staff(true);
		com.bookflow.backend.service.Service service = service();
		StaffWeeklyHours hours = weeklyHours(staff, HOURS_ID, NINE, NOON);
		Appointment appointment = mock(Appointment.class);
		when(appointment.getStartTime()).thenReturn(Instant.parse("2026-09-14T09:30:00Z"));
		when(appointment.getEndTime()).thenReturn(Instant.parse("2026-09-14T10:00:00Z"));
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(currentUserProvider.getRole()).thenReturn(Role.OWNER);
		when(serviceRepository.findByIdAndTenantId(SERVICE_ID, TENANT_ID))
				.thenReturn(Optional.of(service));
		when(weeklyHoursRepository
				.findAllByTenantIdAndStaffIdOrderByDayOfWeekAscStartTimeAsc(
						TENANT_ID,
						STAFF_ID))
				.thenReturn(List.of(hours));
		when(exceptionRepository
				.findAllByTenantIdAndStaffIdAndExceptionDateBetweenOrderByExceptionDateAscStartTimeAsc(
						TENANT_ID,
						STAFF_ID,
						MONDAY,
						MONDAY))
				.thenReturn(List.of());
		when(appointmentRepository
				.findAllByTenantIdAndStaffIdAndStatusNotAndEndTimeGreaterThanAndStartTimeLessThan(
						any(),
						any(),
						any(),
						any(),
						any()))
				.thenReturn(List.of(appointment));

		List<AvailableSlot> result = availabilityService.getAvailableSlots(
				TENANT_ID,
				STAFF_ID,
				SERVICE_ID,
				MONDAY,
				MONDAY);

		assertTrue(result.stream().noneMatch(slot ->
				slot.startTime().equals(Instant.parse("2026-09-14T09:30:00Z"))));
		assertTrue(result.stream().anyMatch(slot ->
				slot.startTime().equals(Instant.parse("2026-09-14T10:00:00Z"))));
	}

	@Test
	void slotRangesCannotExceedTheConfiguredMaximum() {
		Staff staff = staff(true);
		com.bookflow.backend.service.Service service = service();

		assertThrows(
				InvalidOperationException.class,
				() -> availabilityService.calculateSlots(
						TENANT_ID,
						staff,
						service,
						MONDAY,
						MONDAY.plusDays(AvailabilitySlotCalculator.MAX_SLOT_RANGE_DAYS)));
	}

	@Test
	void requestedSlotMustMatchARealCalculatedSlot() {
		StaffWeeklyHours hours =
				weeklyHours(staff(true), HOURS_ID, NINE, NOON);
		when(weeklyHoursRepository
				.findAllByTenantIdAndStaffIdOrderByDayOfWeekAscStartTimeAsc(
						TENANT_ID,
						STAFF_ID))
				.thenReturn(List.of(hours));
		when(exceptionRepository
				.findAllByTenantIdAndStaffIdAndExceptionDateBetweenOrderByExceptionDateAscStartTimeAsc(
						TENANT_ID,
						STAFF_ID,
						MONDAY,
						MONDAY))
				.thenReturn(List.of());

		availabilityService.assertRequestedSlotIsAvailable(
				TENANT_ID,
				STAFF_ID,
				30,
				Instant.parse("2026-09-14T09:00:00Z"));
		assertThrows(
				InvalidOperationException.class,
				() -> availabilityService.assertRequestedSlotIsAvailable(
						TENANT_ID,
						STAFF_ID,
						30,
						Instant.parse("2026-09-14T09:10:00Z")));
	}

	private Staff staff(boolean active) {
		Staff staff = mock(Staff.class);
		lenient().when(staff.getId()).thenReturn(STAFF_ID);
		lenient().when(staff.isActive()).thenReturn(active);
		lenient().when(staff.getTenant()).thenReturn(new Tenant(
				"Glow Studio",
				"hello@example.com",
				null));
		return staff;
	}

	private StaffWeeklyHours weeklyHours(
			Staff staff,
			Long id,
			LocalTime startTime,
			LocalTime endTime) {
		StaffWeeklyHours hours = mock(StaffWeeklyHours.class);
		lenient().when(hours.getId()).thenReturn(id);
		lenient().when(hours.getStaff()).thenReturn(staff);
		lenient().when(hours.getDayOfWeek()).thenReturn(DayOfWeek.MONDAY);
		lenient().when(hours.getStartTime()).thenReturn(startTime);
		lenient().when(hours.getEndTime()).thenReturn(endTime);
		return hours;
	}

	private StaffAvailabilityException availabilityException(
			Staff staff,
			Long id,
			LocalDate date,
			LocalTime startTime,
			LocalTime endTime) {
		StaffAvailabilityException exception = mock(StaffAvailabilityException.class);
		lenient().when(exception.getId()).thenReturn(id);
		lenient().when(exception.getStaff()).thenReturn(staff);
		lenient().when(exception.getExceptionDate()).thenReturn(date);
		lenient().when(exception.getType()).thenReturn(AvailabilityExceptionType.CUSTOM_HOURS);
		lenient().when(exception.getStartTime()).thenReturn(startTime);
		lenient().when(exception.getEndTime()).thenReturn(endTime);
		return exception;
	}

	private com.bookflow.backend.service.Service service() {
		return new com.bookflow.backend.service.Service(
				new Tenant("Glow Studio", "hello@example.com", null),
				"Haircut",
				null,
				BigDecimal.valueOf(75),
				30);
	}
}
