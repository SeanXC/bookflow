package com.bookflow.backend.publicbooking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bookflow.backend.appointment.Appointment;
import com.bookflow.backend.appointment.AppointmentService;
import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.customer.Customer;
import com.bookflow.backend.customer.CustomerRepository;
import com.bookflow.backend.publicbooking.dto.PublicAppointmentRequest;
import com.bookflow.backend.service.ServiceRepository;
import com.bookflow.backend.staff.Staff;
import com.bookflow.backend.staff.StaffRepository;
import com.bookflow.backend.tenant.Tenant;

@ExtendWith(MockitoExtension.class)
class PublicBookingServiceTest {

	private static final String SLUG = "glow-studio";
	private static final Long TENANT_ID = 10L;
	private static final Long CUSTOMER_ID = 30L;
	private static final Long STAFF_ID = 40L;
	private static final Long SERVICE_ID = 50L;
	private static final Instant START_TIME = Instant.parse("2026-09-14T09:00:00Z");

	@Mock
	private PublicProfileService publicProfileService;

	@Mock
	private CustomerRepository customerRepository;

	@Mock
	private StaffRepository staffRepository;

	@Mock
	private ServiceRepository serviceRepository;

	@Mock
	private AppointmentService appointmentService;

	private PublicBookingService publicBookingService;
	private Tenant tenant;

	@BeforeEach
	void setUp() {
		publicBookingService = new PublicBookingService(
				publicProfileService,
				customerRepository,
				staffRepository,
				serviceRepository,
				appointmentService);
		tenant = new Tenant("Glow Studio", "hello@example.com", null);
		ReflectionTestUtils.setField(tenant, "id", TENANT_ID);
	}

	@Test
	void createPublicAppointmentCreatesACustomerAndDelegatesBooking() {
		Staff staff = new Staff(tenant, null, "Anna", "Smith", null);
		com.bookflow.backend.service.Service service = haircut();
		when(publicProfileService.getPublicBusiness(SLUG)).thenReturn(tenant);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(serviceRepository.findByIdAndTenantId(SERVICE_ID, TENANT_ID))
				.thenReturn(Optional.of(service));
		when(customerRepository.findFirstByTenantIdAndEmailIgnoreCase(
				TENANT_ID,
				"emma@example.com"))
				.thenReturn(Optional.empty());
		when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
			Customer customer = invocation.getArgument(0);
			ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);
			return customer;
		});
		Appointment appointment = appointment(new Customer(
				tenant,
				"Emma",
				"Smith",
				"emma@example.com",
				"0871112222",
				null));
		when(appointmentService.bookAppointment(
				TENANT_ID,
				CUSTOMER_ID,
				STAFF_ID,
				SERVICE_ID,
				START_TIME,
				"Please use the side door"))
				.thenReturn(appointment);

		Appointment created = publicBookingService.createPublicAppointment(
				SLUG,
				request(" Emma ", " Smith ", " emma@example.com ", " 0871112222 ",
						" Please use the side door "));

		assertSame(appointment, created);
		ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
		verify(customerRepository).save(customerCaptor.capture());
		assertEquals("Emma", customerCaptor.getValue().getFirstName());
		assertEquals("Smith", customerCaptor.getValue().getLastName());
		assertEquals("emma@example.com", customerCaptor.getValue().getEmail());
		assertEquals("0871112222", customerCaptor.getValue().getPhone());
		assertNull(customerCaptor.getValue().getNotes());
		verify(appointmentService).bookAppointment(
				TENANT_ID,
				CUSTOMER_ID,
				STAFF_ID,
				SERVICE_ID,
				START_TIME,
				"Please use the side door");
	}

	@Test
	void createPublicAppointmentReusesACustomerMatchedByEmail() {
		Staff staff = new Staff(tenant, null, "Anna", "Smith", null);
		Customer existing = new Customer(
				tenant,
				"Old",
				"Name",
				"emma@example.com",
				"000",
				"VIP");
		ReflectionTestUtils.setField(existing, "id", CUSTOMER_ID);
		when(publicProfileService.getPublicBusiness(SLUG)).thenReturn(tenant);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(serviceRepository.findByIdAndTenantId(SERVICE_ID, TENANT_ID))
				.thenReturn(Optional.of(haircut()));
		when(customerRepository.findFirstByTenantIdAndEmailIgnoreCase(
				TENANT_ID,
				"emma@example.com"))
				.thenReturn(Optional.of(existing));
		when(appointmentService.bookAppointment(
				eq(TENANT_ID),
				eq(CUSTOMER_ID),
				eq(STAFF_ID),
				eq(SERVICE_ID),
				eq(START_TIME),
				eq(null)))
				.thenReturn(appointment(existing));

		publicBookingService.createPublicAppointment(
				SLUG,
				request("Emma", "Smith", "emma@example.com", "0871112222", "   "));

		assertEquals("Emma", existing.getFirstName());
		assertEquals("Smith", existing.getLastName());
		assertEquals("emma@example.com", existing.getEmail());
		assertEquals("0871112222", existing.getPhone());
		assertEquals("VIP", existing.getNotes());
		verify(customerRepository, never()).save(any(Customer.class));
	}

	@Test
	void createPublicAppointmentHidesInactiveStaff() {
		Staff staff = new Staff(tenant, null, "Anna", "Smith", null);
		staff.deactivate();
		when(publicProfileService.getPublicBusiness(SLUG)).thenReturn(tenant);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));

		assertThrows(
				ResourceNotFoundException.class,
				() -> publicBookingService.createPublicAppointment(SLUG, request(
						"Emma",
						"Smith",
						"emma@example.com",
						"0871112222",
						null)));

		verifyNoInteractions(appointmentService);
		verify(customerRepository, never()).save(any(Customer.class));
		verify(serviceRepository, never()).findByIdAndTenantId(anyLong(), anyLong());
	}

	@Test
	void createPublicAppointmentHidesInactiveServices() {
		when(publicProfileService.getPublicBusiness(SLUG)).thenReturn(tenant);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(new Staff(tenant, null, "Anna", "Smith", null)));
		com.bookflow.backend.service.Service service = haircut();
		service.deactivate();
		when(serviceRepository.findByIdAndTenantId(SERVICE_ID, TENANT_ID))
				.thenReturn(Optional.of(service));

		assertThrows(
				ResourceNotFoundException.class,
				() -> publicBookingService.createPublicAppointment(SLUG, request(
						"Emma",
						"Smith",
						"emma@example.com",
						"0871112222",
						null)));

		verifyNoInteractions(appointmentService);
		verify(customerRepository, never()).findFirstByTenantIdAndEmailIgnoreCase(
				anyLong(),
				anyString());
	}

	@Test
	void createPublicAppointmentRequiresAPublicBusiness() {
		when(publicProfileService.getPublicBusiness(SLUG))
				.thenThrow(new ResourceNotFoundException("Business", SLUG));

		assertThrows(
				ResourceNotFoundException.class,
				() -> publicBookingService.createPublicAppointment(SLUG, request(
						"Emma",
						"Smith",
						"emma@example.com",
						"0871112222",
						null)));

		verifyNoInteractions(staffRepository, serviceRepository, customerRepository, appointmentService);
	}

	private PublicAppointmentRequest request(
			String firstName,
			String lastName,
			String email,
			String phone,
			String notes) {
		return new PublicAppointmentRequest(
				STAFF_ID,
				SERVICE_ID,
				START_TIME,
				firstName,
				lastName,
				email,
				phone,
				notes);
	}

	private Appointment appointment(Customer customer) {
		Appointment appointment = new Appointment(
				tenant,
				customer,
				new Staff(tenant, null, "Anna", "Smith", null),
				haircut(),
				START_TIME,
				START_TIME.plusSeconds(3600),
				"Please use the side door");
		ReflectionTestUtils.setField(appointment, "id", 99L);
		return appointment;
	}

	private com.bookflow.backend.service.Service haircut() {
		return new com.bookflow.backend.service.Service(
				tenant,
				"Haircut",
				null,
				new BigDecimal("30.00"),
				60);
	}
}
