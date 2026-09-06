package com.bookflow.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.bookflow.backend.appointment.Appointment;
import com.bookflow.backend.appointment.AppointmentRepository;
import com.bookflow.backend.auth.AuthService;
import com.bookflow.backend.auth.dto.AuthResponse;
import com.bookflow.backend.auth.dto.RegisterRequest;
import com.bookflow.backend.customer.Customer;
import com.bookflow.backend.customer.CustomerRepository;
import com.bookflow.backend.security.AuthenticatedUser;
import com.bookflow.backend.security.JwtTokenService;
import com.bookflow.backend.service.ServiceRepository;
import com.bookflow.backend.staff.Staff;
import com.bookflow.backend.staff.StaffRepository;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;
import com.bookflow.backend.user.Role;
import com.bookflow.backend.user.User;
import com.bookflow.backend.user.UserRepository;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {
	"bookflow.security.jwt.secret=test-only-secret-at-least-32-characters"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AppointmentWorkflowIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private AuthService authService;

	@Autowired
	private JwtTokenService jwtTokenService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private StaffRepository staffRepository;

	@Autowired
	private ServiceRepository serviceRepository;

	@Autowired
	private AppointmentRepository appointmentRepository;

	@BeforeEach
	@AfterEach
	void cleanDatabase() {
		appointmentRepository.deleteAllInBatch();
		staffRepository.deleteAllInBatch();
		serviceRepository.deleteAllInBatch();
		customerRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
		tenantRepository.deleteAllInBatch();
	}

	@Test
	void createAppointmentPersistsBackendCalculatedEndTime() throws Exception {
		BookingFixture fixture = createFixture(90);

		mockMvc.perform(post("/api/appointments")
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(appointmentPayload(fixture, "2026-09-12T14:00:00Z")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.startTime").value("2026-09-12T14:00:00Z"))
			.andExpect(jsonPath("$.endTime").value("2026-09-12T15:30:00Z"))
			.andExpect(jsonPath("$.status").value("CONFIRMED"));

		assertEquals(1, appointmentRepository.count());
	}

	@Test
	void overlappingAppointmentReturnsBookingConflict() throws Exception {
		BookingFixture fixture = createFixture(60);
		createAppointment(fixture, "2026-09-12T14:00:00Z");

		mockMvc.perform(post("/api/appointments")
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(appointmentPayload(fixture, "2026-09-12T14:30:00Z")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("BOOKING_CONFLICT"));

		assertEquals(1, appointmentRepository.count());
	}

	@Test
	void cancelledAppointmentNoLongerBlocksTheSameTime() throws Exception {
		BookingFixture fixture = createFixture(60);
		long appointmentId = createAppointment(fixture, "2026-09-12T14:00:00Z");

		mockMvc.perform(patch("/api/appointments/{appointmentId}/status", appointmentId)
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "status": "CANCELLED"
						}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELLED"));

		mockMvc.perform(post("/api/appointments")
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(appointmentPayload(fixture, "2026-09-12T14:00:00Z")))
			.andExpect(status().isCreated());

		assertEquals(2, appointmentRepository.count());
	}

	@Test
	void confirmedAppointmentCanBeRescheduled() throws Exception {
		BookingFixture fixture = createFixture(60);
		long appointmentId = createAppointment(fixture, "2026-09-12T14:00:00Z");

		mockMvc.perform(put("/api/appointments/{appointmentId}", appointmentId)
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(appointmentPayload(fixture, "2026-09-13T09:00:00Z")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.startTime").value("2026-09-13T09:00:00Z"))
			.andExpect(jsonPath("$.endTime").value("2026-09-13T10:00:00Z"));
	}

	@Test
	void staffCanOnlyListAndOpenTheirOwnAppointments() throws Exception {
		BookingFixture fixture = createFixture(60);
		StaffIdentity staffA = createStaffIdentity(fixture.tenant(), "anna@example.com");
		StaffIdentity staffB = createStaffIdentity(fixture.tenant(), "sophie@example.com");
		Appointment appointmentA = saveAppointment(
				fixture,
				staffA.staff(),
				Instant.parse("2026-09-12T14:00:00Z"));
		Appointment appointmentB = saveAppointment(
				fixture,
				staffB.staff(),
				Instant.parse("2026-09-12T16:00:00Z"));

		mockMvc.perform(get("/api/appointments")
				.header(HttpHeaders.AUTHORIZATION, bearer(staffA.token())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].id").value(appointmentA.getId()))
			.andExpect(jsonPath("$.content[0].staffId").value(staffA.staff().getId()));

		mockMvc.perform(get("/api/appointments/{appointmentId}", appointmentB.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(staffA.token())))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error").value("FORBIDDEN"));
	}

	@Test
	void concurrentRequestsCannotDoubleBookTheSameStaffAndTime() throws Exception {
		BookingFixture fixture = createFixture(60);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);

		try {
			Future<Integer> first = executor.submit(
					() -> createConcurrentAppointment(fixture, ready, start));
			Future<Integer> second = executor.submit(
					() -> createConcurrentAppointment(fixture, ready, start));

			assertTrue(ready.await(10, TimeUnit.SECONDS));
			start.countDown();
			List<Integer> statuses = List.of(
					first.get(20, TimeUnit.SECONDS),
					second.get(20, TimeUnit.SECONDS))
					.stream()
					.sorted()
					.toList();

			assertEquals(List.of(201, 409), statuses);
			assertEquals(1, appointmentRepository.count());
		} finally {
			start.countDown();
			executor.shutdownNow();
		}
	}

	private int createConcurrentAppointment(
			BookingFixture fixture,
			CountDownLatch ready,
			CountDownLatch start) throws Exception {
		ready.countDown();
		if (!start.await(10, TimeUnit.SECONDS)) {
			throw new IllegalStateException("Timed out waiting to start concurrent request");
		}
		return mockMvc.perform(post("/api/appointments")
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(appointmentPayload(fixture, "2026-09-12T14:00:00Z")))
			.andReturn()
			.getResponse()
			.getStatus();
	}

	private BookingFixture createFixture(int durationMinutes) {
		AuthResponse owner = authService.register(new RegisterRequest(
				"Glow Studio",
				"hello@example.com",
				null,
				"owner@example.com",
				"password123"));
		Tenant tenant = tenantRepository.findAll().getFirst();
		Customer customer = customerRepository.saveAndFlush(new Customer(
				tenant,
				"Emma",
				"Smith",
				"emma@example.com",
				null,
				null));
		Staff staff = staffRepository.saveAndFlush(new Staff(
				tenant,
				null,
				"Anna",
				"Smith",
				null));
		com.bookflow.backend.service.Service service =
				serviceRepository.saveAndFlush(new com.bookflow.backend.service.Service(
						tenant,
						"Haircut",
						null,
						new BigDecimal("30.00"),
						durationMinutes));
		return new BookingFixture(
				owner.accessToken(),
				tenant,
				customer,
				staff,
				service);
	}

	private StaffIdentity createStaffIdentity(Tenant tenant, String email) {
		User user = userRepository.saveAndFlush(new User(
				tenant,
				email,
				passwordEncoder.encode("password123"),
				Role.STAFF));
		Staff staff = staffRepository.saveAndFlush(new Staff(
				tenant,
				user,
				email.substring(0, email.indexOf('@')),
				"Smith",
				null));
		String token = jwtTokenService.createAccessToken(AuthenticatedUser.from(user));
		return new StaffIdentity(staff, token);
	}

	private Appointment saveAppointment(
			BookingFixture fixture,
			Staff staff,
			Instant startTime) {
		return appointmentRepository.saveAndFlush(new Appointment(
				fixture.tenant(),
				fixture.customer(),
				staff,
				fixture.service(),
				startTime,
				startTime.plusSeconds(fixture.service().getDurationMinutes() * 60L),
				null));
	}

	private long createAppointment(BookingFixture fixture, String startTime) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/appointments")
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(appointmentPayload(fixture, startTime)))
			.andExpect(status().isCreated())
			.andReturn();
		return jsonMapper.readTree(result.getResponse().getContentAsString())
				.get("id")
				.asLong();
	}

	private String appointmentPayload(BookingFixture fixture, String startTime) {
		return """
				{
				  "customerId": %d,
				  "staffId": %d,
				  "serviceId": %d,
				  "startTime": "%s",
				  "notes": "Integration test"
				}
				""".formatted(
					fixture.customer().getId(),
					fixture.staff().getId(),
					fixture.service().getId(),
					startTime);
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}

	private record BookingFixture(
			String ownerToken,
			Tenant tenant,
			Customer customer,
			Staff staff,
			com.bookflow.backend.service.Service service) {
	}

	private record StaffIdentity(Staff staff, String token) {
	}
}
