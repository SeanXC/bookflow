package com.bookflow.backend.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bookflow.backend.appointment.AppointmentRepository;
import com.bookflow.backend.appointment.AppointmentStatus;
import com.bookflow.backend.availability.AvailabilityService;
import com.bookflow.backend.availability.AvailableSlot;
import com.bookflow.backend.common.exception.InvalidOperationException;
import com.bookflow.backend.service.ServiceRepository;
import com.bookflow.backend.staff.Staff;
import com.bookflow.backend.staff.StaffRepository;
import com.bookflow.backend.tenant.Tenant;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class BookingToolExecutorTest {

	private static final Long TENANT_ID = 10L;
	private static final Long STAFF_ID = 40L;
	private static final Long SERVICE_ID = 50L;
	private static final Instant START_TIME = Instant.parse("2026-09-14T09:00:00Z");
	private static final Instant END_TIME = Instant.parse("2026-09-14T10:00:00Z");
	private static final LocalDate SLOT_DATE = LocalDate.of(2026, 9, 14);

	@Mock
	private ServiceRepository serviceRepository;

	@Mock
	private StaffRepository staffRepository;

	@Mock
	private AvailabilityService availabilityService;

	@Mock
	private AppointmentRepository appointmentRepository;

	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private BookingToolExecutor executor;
	private Tenant tenant;

	@BeforeEach
	void setUp() {
		executor = new BookingToolExecutor(
				serviceRepository,
				staffRepository,
				availabilityService,
				appointmentRepository,
				jsonMapper);
		tenant = new Tenant("Glow Studio", "hello@example.com", null);
		ReflectionTestUtils.setField(tenant, "id", TENANT_ID);
	}

	@Test
	void listServicesUsesTheTenantAndActiveFilter() {
		com.bookflow.backend.service.Service service = haircut();
		when(serviceRepository.findAllByTenantIdAndActiveOrderByNameAscIdAsc(TENANT_ID, true))
				.thenReturn(List.of(service));

		JsonNode result = jsonMapper.readTree(executor.execute(
				TENANT_ID,
				BookingToolContract.LIST_SERVICES,
				"{}"));

		assertEquals(1, result.size());
		assertEquals(SERVICE_ID, result.path(0).path("id").asLong());
		assertEquals("Haircut", result.path(0).path("name").asString());
		assertEquals(60L, result.path(0).path("durationMinutes").asLong());
		verify(serviceRepository).findAllByTenantIdAndActiveOrderByNameAscIdAsc(TENANT_ID, true);
		verifyNoInteractions(availabilityService, appointmentRepository);
	}

	@Test
	void listStaffUsesTheTenantAndActiveFilter() {
		Staff staff = anna();
		when(staffRepository.findAllByTenantIdAndActiveOrderByLastNameAscFirstNameAscIdAsc(
				TENANT_ID,
				true))
				.thenReturn(List.of(staff));

		JsonNode result = jsonMapper.readTree(executor.execute(
				TENANT_ID,
				new LlmToolCall("call_1", BookingToolContract.LIST_STAFF, "{}")));

		assertEquals(1, result.size());
		assertEquals(STAFF_ID, result.path(0).path("id").asLong());
		assertEquals("Anna", result.path(0).path("firstName").asString());
		assertEquals("Smith", result.path(0).path("lastName").asString());
		verifyNoInteractions(availabilityService, appointmentRepository);
	}

	@Test
	void listSlotsDelegatesToAvailabilityCalculation() {
		Staff staff = anna();
		com.bookflow.backend.service.Service service = haircut();
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(serviceRepository.findByIdAndTenantId(SERVICE_ID, TENANT_ID))
				.thenReturn(Optional.of(service));
		when(availabilityService.calculateSlots(
				TENANT_ID,
				staff,
				service,
				SLOT_DATE,
				SLOT_DATE))
				.thenReturn(List.of(new AvailableSlot(START_TIME, END_TIME)));

		JsonNode result = jsonMapper.readTree(executor.execute(
				TENANT_ID,
				BookingToolContract.LIST_SLOTS,
				"""
				{"staffId":40,"serviceId":50,"from":"2026-09-14","to":"2026-09-14"}
				"""));

		assertEquals(START_TIME.toString(), result.path(0).path("startTime").asString());
		assertEquals(END_TIME.toString(), result.path(0).path("endTime").asString());
		verify(availabilityService).calculateSlots(
				TENANT_ID,
				staff,
				service,
				SLOT_DATE,
				SLOT_DATE);
		verifyNoInteractions(appointmentRepository);
	}

	@Test
	void listSlotsRejectsInactiveStaffWithoutCallingAvailability() {
		Staff staff = anna();
		staff.deactivate();
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));

		JsonNode result = jsonMapper.readTree(executor.execute(
				TENANT_ID,
				BookingToolContract.LIST_SLOTS,
				"""
				{"staffId":40,"serviceId":50,"from":"2026-09-14","to":"2026-09-14"}
				"""));

		assertEquals("Staff not found: 40", result.path("error").asString());
		verifyNoInteractions(availabilityService, appointmentRepository);
	}

	@Test
	void listSlotsReturnsToolErrorForInvalidArguments() {
		JsonNode result = jsonMapper.readTree(executor.execute(
				TENANT_ID,
				BookingToolContract.LIST_SLOTS,
				"""
				{"staffId":"anna","serviceId":50}
				"""));

		assertEquals("Invalid tool arguments", result.path("error").asString());
		verifyNoInteractions(staffRepository, serviceRepository, availabilityService);
	}

	@Test
	void proposeBookingValidatesARealSlotWithoutCreatingAnAppointment() {
		Staff staff = anna();
		com.bookflow.backend.service.Service service = haircut();
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(serviceRepository.findByIdAndTenantId(SERVICE_ID, TENANT_ID))
				.thenReturn(Optional.of(service));
		when(appointmentRepository.countConflictingAppointments(
				TENANT_ID,
				STAFF_ID,
				AppointmentStatus.CANCELLED,
				START_TIME,
				END_TIME))
				.thenReturn(0L);

		JsonNode result = jsonMapper.readTree(executor.execute(
				TENANT_ID,
				BookingToolContract.PROPOSE_BOOKING,
				proposeArguments()));

		assertEquals("proposed", result.path("status").asString());
		assertTrue(result.path("requiresConfirmation").asBoolean());
		assertEquals(STAFF_ID, result.path("staffId").asLong());
		assertEquals("Anna", result.path("staffFirstName").asString());
		assertEquals(SERVICE_ID, result.path("serviceId").asLong());
		assertEquals("Haircut", result.path("serviceName").asString());
		assertEquals(START_TIME.toString(), result.path("startTime").asString());
		assertEquals(END_TIME.toString(), result.path("endTime").asString());
		assertEquals("Emma", result.path("firstName").asString());
		assertEquals("emma@example.com", result.path("email").asString());
		assertFalse(result.has("appointmentId"));
		verify(availabilityService).assertRequestedSlotIsAvailable(
				TENANT_ID,
				STAFF_ID,
				60,
				START_TIME);
		verify(appointmentRepository, never()).save(any());
	}

	@Test
	void proposeBookingReturnsToolErrorWhenTheSlotIsNotBookable() {
		Staff staff = anna();
		com.bookflow.backend.service.Service service = haircut();
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(serviceRepository.findByIdAndTenantId(SERVICE_ID, TENANT_ID))
				.thenReturn(Optional.of(service));
		doThrow(new InvalidOperationException(
				"The requested start time is not an available booking slot"))
				.when(availabilityService)
				.assertRequestedSlotIsAvailable(TENANT_ID, STAFF_ID, 60, START_TIME);

		JsonNode result = jsonMapper.readTree(executor.execute(
				TENANT_ID,
				BookingToolContract.PROPOSE_BOOKING,
				proposeArguments()));

		assertEquals(
				"The requested start time is not an available booking slot",
				result.path("error").asString());
		verify(appointmentRepository, never()).countConflictingAppointments(
				any(),
				any(),
				any(),
				any(),
				any());
		verify(appointmentRepository, never()).save(any());
	}

	@Test
	void proposeBookingReturnsToolErrorWhenTheSlotConflicts() {
		Staff staff = anna();
		com.bookflow.backend.service.Service service = haircut();
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(serviceRepository.findByIdAndTenantId(SERVICE_ID, TENANT_ID))
				.thenReturn(Optional.of(service));
		when(appointmentRepository.countConflictingAppointments(
				TENANT_ID,
				STAFF_ID,
				AppointmentStatus.CANCELLED,
				START_TIME,
				END_TIME))
				.thenReturn(1L);

		JsonNode result = jsonMapper.readTree(executor.execute(
				TENANT_ID,
				BookingToolContract.PROPOSE_BOOKING,
				proposeArguments()));

		assertEquals(
				"This staff member already has an appointment during the selected time.",
				result.path("error").asString());
		verify(appointmentRepository, never()).save(any());
	}

	@Test
	void proposeBookingRejectsInactiveServices() {
		Staff staff = anna();
		com.bookflow.backend.service.Service service = haircut();
		service.deactivate();
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(serviceRepository.findByIdAndTenantId(SERVICE_ID, TENANT_ID))
				.thenReturn(Optional.of(service));

		JsonNode result = jsonMapper.readTree(executor.execute(
				TENANT_ID,
				BookingToolContract.PROPOSE_BOOKING,
				proposeArguments()));

		assertEquals("Service not found: 50", result.path("error").asString());
		verifyNoInteractions(availabilityService);
		verify(appointmentRepository, never()).save(any());
	}

	@Test
	void unknownToolThrowsWithoutTouchingTenantData() {
		assertThrows(
				InvalidOperationException.class,
				() -> executor.execute(TENANT_ID, "delete_tenant", "{}"));

		verifyNoInteractions(
				serviceRepository,
				staffRepository,
				availabilityService,
				appointmentRepository);
	}

	@Test
	void malformedJsonReturnsAToolError() {
		JsonNode result = jsonMapper.readTree(executor.execute(
				TENANT_ID,
				BookingToolContract.LIST_SERVICES,
				"{not-json"));

		assertEquals("Invalid tool arguments", result.path("error").asString());
		verifyNoInteractions(serviceRepository);
	}

	private Staff anna() {
		Staff staff = new Staff(tenant, null, "Anna", "Smith", null);
		ReflectionTestUtils.setField(staff, "id", STAFF_ID);
		return staff;
	}

	private com.bookflow.backend.service.Service haircut() {
		com.bookflow.backend.service.Service service = new com.bookflow.backend.service.Service(
				tenant,
				"Haircut",
				null,
				new BigDecimal("30.00"),
				60);
		ReflectionTestUtils.setField(service, "id", SERVICE_ID);
		return service;
	}

	private String proposeArguments() {
		return """
				{
				  "staffId": 40,
				  "serviceId": 50,
				  "startTime": "2026-09-14T09:00:00Z",
				  "firstName": "Emma",
				  "lastName": "Chen",
				  "email": "emma@example.com",
				  "phone": "555-0100"
				}
				""";
	}
}
