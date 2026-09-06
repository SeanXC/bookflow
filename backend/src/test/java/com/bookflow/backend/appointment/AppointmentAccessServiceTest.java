package com.bookflow.backend.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import com.bookflow.backend.common.exception.InvalidOperationException;
import com.bookflow.backend.customer.Customer;
import com.bookflow.backend.customer.CustomerRepository;
import com.bookflow.backend.security.CurrentUserProvider;
import com.bookflow.backend.service.ServiceRepository;
import com.bookflow.backend.staff.Staff;
import com.bookflow.backend.staff.StaffRepository;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;
import com.bookflow.backend.user.Role;
import com.bookflow.backend.user.User;

@ExtendWith(MockitoExtension.class)
class AppointmentAccessServiceTest {

	private static final Long TENANT_ID = 10L;
	private static final Long APPOINTMENT_ID = 20L;
	private static final Long USER_ID = 30L;
	private static final Instant START_TIME = Instant.parse("2026-09-12T14:00:00Z");

	@Mock
	private AppointmentRepository appointmentRepository;

	@Mock
	private TenantRepository tenantRepository;

	@Mock
	private CustomerRepository customerRepository;

	@Mock
	private StaffRepository staffRepository;

	@Mock
	private ServiceRepository serviceRepository;

	@Mock
	private CurrentUserProvider currentUserProvider;

	private AppointmentService appointmentService;

	@BeforeEach
	void setUp() {
		appointmentService = new AppointmentService(
				appointmentRepository,
				tenantRepository,
				customerRepository,
				staffRepository,
				serviceRepository,
				currentUserProvider);
	}

	@Test
	void staffCanGetAnAppointmentLinkedToTheirUserAccount() {
		Appointment appointment = appointment(staffWithUser(USER_ID));
		when(appointmentRepository.findByIdAndTenantId(APPOINTMENT_ID, TENANT_ID))
				.thenReturn(Optional.of(appointment));
		when(currentUserProvider.getRole()).thenReturn(Role.STAFF);
		when(currentUserProvider.getUserId()).thenReturn(USER_ID);

		Appointment result = appointmentService.getAppointment(TENANT_ID, APPOINTMENT_ID);

		assertSame(appointment, result);
	}

	@Test
	void staffCannotGetAnotherStaffMembersAppointment() {
		Appointment appointment = appointment(staffWithUser(99L));
		when(appointmentRepository.findByIdAndTenantId(APPOINTMENT_ID, TENANT_ID))
				.thenReturn(Optional.of(appointment));
		when(currentUserProvider.getRole()).thenReturn(Role.STAFF);
		when(currentUserProvider.getUserId()).thenReturn(USER_ID);

		assertThrows(
				AccessDeniedException.class,
				() -> appointmentService.getAppointment(TENANT_ID, APPOINTMENT_ID));
	}

	@Test
	void staffCannotGetAnAppointmentWithoutALinkedUserAccount() {
		Appointment appointment = appointment(new Staff(
				tenant(),
				null,
				"Anna",
				"Smith",
				null));
		when(appointmentRepository.findByIdAndTenantId(APPOINTMENT_ID, TENANT_ID))
				.thenReturn(Optional.of(appointment));
		when(currentUserProvider.getRole()).thenReturn(Role.STAFF);

		assertThrows(
				AccessDeniedException.class,
				() -> appointmentService.getAppointment(TENANT_ID, APPOINTMENT_ID));
	}

	@Test
	void staffCanCompleteTheirOwnConfirmedAppointment() {
		Appointment appointment = appointment(staffWithUser(USER_ID));
		when(appointmentRepository.findByIdAndTenantId(APPOINTMENT_ID, TENANT_ID))
				.thenReturn(Optional.of(appointment));
		when(currentUserProvider.getRole()).thenReturn(Role.STAFF);
		when(currentUserProvider.getUserId()).thenReturn(USER_ID);

		Appointment result = appointmentService.updateStatus(
				TENANT_ID,
				APPOINTMENT_ID,
				AppointmentStatus.COMPLETED);

		assertSame(appointment, result);
		assertEquals(AppointmentStatus.COMPLETED, result.getStatus());
	}

	@Test
	void ownerCanCancelAConfirmedAppointment() {
		Appointment appointment = appointment(staffWithoutUser());
		when(appointmentRepository.findByIdAndTenantId(APPOINTMENT_ID, TENANT_ID))
				.thenReturn(Optional.of(appointment));
		when(currentUserProvider.getRole()).thenReturn(Role.OWNER);

		Appointment result = appointmentService.updateStatus(
				TENANT_ID,
				APPOINTMENT_ID,
				AppointmentStatus.CANCELLED);

		assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
	}

	@Test
	void completedAppointmentCannotTransitionAgain() {
		Appointment appointment = appointment(staffWithoutUser());
		appointment.updateStatus(AppointmentStatus.COMPLETED);
		when(appointmentRepository.findByIdAndTenantId(APPOINTMENT_ID, TENANT_ID))
				.thenReturn(Optional.of(appointment));
		when(currentUserProvider.getRole()).thenReturn(Role.OWNER);

		assertThrows(
				InvalidOperationException.class,
				() -> appointmentService.updateStatus(
						TENANT_ID,
						APPOINTMENT_ID,
						AppointmentStatus.CANCELLED));

		assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());
	}

	@Test
	void applyingTheCurrentStatusIsIdempotent() {
		Appointment appointment = appointment(staffWithoutUser());
		when(appointmentRepository.findByIdAndTenantId(APPOINTMENT_ID, TENANT_ID))
				.thenReturn(Optional.of(appointment));
		when(currentUserProvider.getRole()).thenReturn(Role.OWNER);

		Appointment result = appointmentService.updateStatus(
				TENANT_ID,
				APPOINTMENT_ID,
				AppointmentStatus.CONFIRMED);

		assertSame(appointment, result);
		assertEquals(AppointmentStatus.CONFIRMED, result.getStatus());
	}

	@Test
	void staffUnfilteredListUsesTheirUserIdAndIgnoresRequestedStaffId() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<Appointment> expected = Page.empty(pageable);
		when(currentUserProvider.getRole()).thenReturn(Role.STAFF);
		when(currentUserProvider.getUserId()).thenReturn(USER_ID);
		when(appointmentRepository.findAllByTenantIdAndStaffUserId(
				TENANT_ID,
				USER_ID,
				pageable))
				.thenReturn(expected);

		Page<Appointment> result = appointmentService.getAllAppointments(
				TENANT_ID,
				999L,
				null,
				null,
				null,
				pageable);

		assertSame(expected, result);
		verify(appointmentRepository, never()).findAllByTenantIdAndFilters(
				any(),
				any(),
				any(),
				any(),
				any(),
				any());
	}

	@Test
	void staffFilteredListStillUsesTheirUserId() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<Appointment> expected = Page.empty(pageable);
		Instant fromTime = Instant.parse("2026-09-01T00:00:00Z");
		Instant toTime = Instant.parse("2026-09-30T23:59:59Z");
		when(currentUserProvider.getRole()).thenReturn(Role.STAFF);
		when(currentUserProvider.getUserId()).thenReturn(USER_ID);
		when(appointmentRepository.findAllByTenantIdAndStaffUserIdAndFilters(
				TENANT_ID,
				USER_ID,
				AppointmentStatus.CONFIRMED,
				fromTime,
				toTime,
				pageable))
				.thenReturn(expected);

		Page<Appointment> result = appointmentService.getAllAppointments(
				TENANT_ID,
				999L,
				AppointmentStatus.CONFIRMED,
				fromTime,
				toTime,
				pageable);

		assertSame(expected, result);
	}

	@Test
	void ownerFilteredListUsesTenantScopedFilters() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<Appointment> expected = Page.empty(pageable);
		when(currentUserProvider.getRole()).thenReturn(Role.OWNER);
		when(appointmentRepository.findAllByTenantIdAndFilters(
				TENANT_ID,
				40L,
				AppointmentStatus.CONFIRMED,
				null,
				null,
				pageable))
				.thenReturn(expected);

		Page<Appointment> result = appointmentService.getAllAppointments(
				TENANT_ID,
				40L,
				AppointmentStatus.CONFIRMED,
				null,
				null,
				pageable);

		assertSame(expected, result);
	}

	@Test
	void appointmentListRejectsAnInvertedTimeRangeBeforeQuerying() {
		Pageable pageable = PageRequest.of(0, 20);
		Instant fromTime = Instant.parse("2026-09-30T00:00:00Z");
		Instant toTime = Instant.parse("2026-09-01T00:00:00Z");

		assertThrows(
				InvalidOperationException.class,
				() -> appointmentService.getAllAppointments(
						TENANT_ID,
						null,
						null,
						fromTime,
						toTime,
						pageable));

		verify(appointmentRepository, never()).findAllByTenantId(any(), any());
		verify(appointmentRepository, never()).findAllByTenantIdAndFilters(
				any(),
				any(),
				any(),
				any(),
				any(),
				any());
	}

	private Appointment appointment(Staff staff) {
		Tenant tenant = staff.getTenant();
		Customer customer = new Customer(
				tenant,
				"Emma",
				"Smith",
				"emma@example.com",
				null,
				null);
		com.bookflow.backend.service.Service service =
				new com.bookflow.backend.service.Service(
						tenant,
						"Haircut",
						null,
						new BigDecimal("30.00"),
						60);
		return new Appointment(
				tenant,
				customer,
				staff,
				service,
				START_TIME,
				START_TIME.plusSeconds(60 * 60),
				null);
	}

	private Staff staffWithUser(Long userId) {
		Tenant tenant = tenant();
		User user = org.mockito.Mockito.mock(User.class);
		when(user.getId()).thenReturn(userId);
		return new Staff(tenant, user, "Anna", "Smith", null);
	}

	private Staff staffWithoutUser() {
		return new Staff(tenant(), null, "Anna", "Smith", null);
	}

	private Tenant tenant() {
		return new Tenant("Glow Studio", "hello@example.com", null);
	}
}
