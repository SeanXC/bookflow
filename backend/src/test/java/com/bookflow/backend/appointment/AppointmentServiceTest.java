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

import com.bookflow.backend.common.exception.AppointmentConflictException;
import com.bookflow.backend.common.exception.InvalidOperationException;
import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.customer.Customer;
import com.bookflow.backend.customer.CustomerRepository;
import com.bookflow.backend.security.CurrentUserProvider;
import com.bookflow.backend.service.ServiceRepository;
import com.bookflow.backend.staff.Staff;
import com.bookflow.backend.staff.StaffRepository;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

	private static final Long TENANT_ID = 10L;
	private static final Long APPOINTMENT_ID = 20L;
	private static final Long CUSTOMER_ID = 30L;
	private static final Long STAFF_ID = 40L;
	private static final Long SERVICE_ID = 50L;
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
	void createAppointmentCalculatesEndTimeAndUsesTenantScopedResources() {
		BookingResources resources = activeResources(90);
		stubResources(resources);
		when(appointmentRepository.countConflictingAppointments(
				TENANT_ID,
				STAFF_ID,
				AppointmentStatus.CANCELLED,
				START_TIME,
				START_TIME.plusSeconds(90 * 60)))
				.thenReturn(0L);
		when(appointmentRepository.save(any(Appointment.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Appointment appointment = appointmentService.createAppointment(
				TENANT_ID,
				CUSTOMER_ID,
				STAFF_ID,
				SERVICE_ID,
				START_TIME,
				"First visit");

		assertSame(resources.tenant(), appointment.getTenant());
		assertSame(resources.customer(), appointment.getCustomer());
		assertSame(resources.staff(), appointment.getStaff());
		assertSame(resources.service(), appointment.getService());
		assertEquals(START_TIME, appointment.getStartTime());
		assertEquals(Instant.parse("2026-09-12T15:30:00Z"), appointment.getEndTime());
		assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());
		assertEquals("First visit", appointment.getNotes());
		verify(appointmentRepository).countConflictingAppointments(
				TENANT_ID,
				STAFF_ID,
				AppointmentStatus.CANCELLED,
				START_TIME,
				Instant.parse("2026-09-12T15:30:00Z"));
	}

	@Test
	void createAppointmentRejectsAnOverlappingBooking() {
		BookingResources resources = activeResources(60);
		stubResources(resources);
		when(appointmentRepository.countConflictingAppointments(
				TENANT_ID,
				STAFF_ID,
				AppointmentStatus.CANCELLED,
				START_TIME,
				START_TIME.plusSeconds(60 * 60)))
				.thenReturn(1L);

		assertThrows(
				AppointmentConflictException.class,
				() -> appointmentService.createAppointment(
						TENANT_ID,
						CUSTOMER_ID,
						STAFF_ID,
						SERVICE_ID,
						START_TIME,
						null));

		verify(appointmentRepository, never()).save(any(Appointment.class));
	}

	@Test
	void createAppointmentRejectsCustomerOutsideTheTenant() {
		Tenant tenant = tenant();
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		when(customerRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
				.thenReturn(Optional.empty());

		assertThrows(
				ResourceNotFoundException.class,
				() -> appointmentService.createAppointment(
						TENANT_ID,
						CUSTOMER_ID,
						STAFF_ID,
						SERVICE_ID,
						START_TIME,
						null));

		verify(staffRepository, never()).findForUpdateByIdAndTenantId(any(), any());
		verify(appointmentRepository, never()).save(any(Appointment.class));
	}

	@Test
	void createAppointmentRejectsInactiveStaff() {
		BookingResources resources = activeResources(60);
		resources.staff().deactivate();
		stubResources(resources);

		assertThrows(
				InvalidOperationException.class,
				() -> appointmentService.createAppointment(
						TENANT_ID,
						CUSTOMER_ID,
						STAFF_ID,
						SERVICE_ID,
						START_TIME,
						null));

		verify(appointmentRepository, never()).countConflictingAppointments(
				any(),
				any(),
				any(),
				any(),
				any());
	}

	@Test
	void createAppointmentRejectsInactiveService() {
		BookingResources resources = activeResources(60);
		resources.service().deactivate();
		stubResources(resources);

		assertThrows(
				InvalidOperationException.class,
				() -> appointmentService.createAppointment(
						TENANT_ID,
						CUSTOMER_ID,
						STAFF_ID,
						SERVICE_ID,
						START_TIME,
						null));

		verify(appointmentRepository, never()).countConflictingAppointments(
				any(),
				any(),
				any(),
				any(),
				any());
	}

	@Test
	void updateAppointmentRecalculatesEndTimeAndExcludesItselfFromConflictCheck() {
		BookingResources resources = activeResources(45);
		Appointment appointment = appointment(resources, START_TIME, 30);
		Instant newStartTime = Instant.parse("2026-09-13T09:00:00Z");
		when(appointmentRepository.findByIdAndTenantId(APPOINTMENT_ID, TENANT_ID))
				.thenReturn(Optional.of(appointment));
		when(customerRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
				.thenReturn(Optional.of(resources.customer()));
		when(staffRepository.findForUpdateByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(resources.staff()));
		when(serviceRepository.findByIdAndTenantId(SERVICE_ID, TENANT_ID))
				.thenReturn(Optional.of(resources.service()));
		when(appointmentRepository.countConflictingAppointmentsExcluding(
				TENANT_ID,
				STAFF_ID,
				APPOINTMENT_ID,
				AppointmentStatus.CANCELLED,
				newStartTime,
				Instant.parse("2026-09-13T09:45:00Z")))
				.thenReturn(0L);

		Appointment updated = appointmentService.updateAppointment(
				TENANT_ID,
				APPOINTMENT_ID,
				CUSTOMER_ID,
				STAFF_ID,
				SERVICE_ID,
				newStartTime,
				"Rescheduled");

		assertSame(appointment, updated);
		assertEquals(newStartTime, updated.getStartTime());
		assertEquals(Instant.parse("2026-09-13T09:45:00Z"), updated.getEndTime());
		assertEquals("Rescheduled", updated.getNotes());
		verify(appointmentRepository).countConflictingAppointmentsExcluding(
				TENANT_ID,
				STAFF_ID,
				APPOINTMENT_ID,
				AppointmentStatus.CANCELLED,
				newStartTime,
				Instant.parse("2026-09-13T09:45:00Z"));
	}

	@Test
	void updateAppointmentRejectsAConflictingTime() {
		BookingResources resources = activeResources(60);
		Appointment appointment = appointment(resources, START_TIME, 60);
		Instant newStartTime = Instant.parse("2026-09-13T09:00:00Z");
		when(appointmentRepository.findByIdAndTenantId(APPOINTMENT_ID, TENANT_ID))
				.thenReturn(Optional.of(appointment));
		when(customerRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
				.thenReturn(Optional.of(resources.customer()));
		when(staffRepository.findForUpdateByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(resources.staff()));
		when(serviceRepository.findByIdAndTenantId(SERVICE_ID, TENANT_ID))
				.thenReturn(Optional.of(resources.service()));
		when(appointmentRepository.countConflictingAppointmentsExcluding(
				TENANT_ID,
				STAFF_ID,
				APPOINTMENT_ID,
				AppointmentStatus.CANCELLED,
				newStartTime,
				newStartTime.plusSeconds(60 * 60)))
				.thenReturn(1L);

		assertThrows(
				AppointmentConflictException.class,
				() -> appointmentService.updateAppointment(
						TENANT_ID,
						APPOINTMENT_ID,
						CUSTOMER_ID,
						STAFF_ID,
						SERVICE_ID,
						newStartTime,
						null));

		assertEquals(START_TIME, appointment.getStartTime());
	}

	private void stubResources(BookingResources resources) {
		when(tenantRepository.findById(TENANT_ID))
				.thenReturn(Optional.of(resources.tenant()));
		when(customerRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
				.thenReturn(Optional.of(resources.customer()));
		when(staffRepository.findForUpdateByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(resources.staff()));
		when(serviceRepository.findByIdAndTenantId(SERVICE_ID, TENANT_ID))
				.thenReturn(Optional.of(resources.service()));
	}

	private BookingResources activeResources(int durationMinutes) {
		Tenant tenant = tenant();
		Customer customer = new Customer(
				tenant,
				"Emma",
				"Smith",
				"emma@example.com",
				null,
				null);
		Staff staff = new Staff(tenant, null, "Anna", "Smith", null);
		com.bookflow.backend.service.Service service =
				new com.bookflow.backend.service.Service(
						tenant,
						"Haircut",
						null,
						new BigDecimal("30.00"),
						durationMinutes);
		return new BookingResources(tenant, customer, staff, service);
	}

	private Appointment appointment(
			BookingResources resources,
			Instant startTime,
			int durationMinutes) {
		return new Appointment(
				resources.tenant(),
				resources.customer(),
				resources.staff(),
				resources.service(),
				startTime,
				startTime.plusSeconds(durationMinutes * 60L),
				null);
	}

	private Tenant tenant() {
		return new Tenant("Glow Studio", "hello@example.com", null);
	}

	private record BookingResources(
			Tenant tenant,
			Customer customer,
			Staff staff,
			com.bookflow.backend.service.Service service) {
	}
}
