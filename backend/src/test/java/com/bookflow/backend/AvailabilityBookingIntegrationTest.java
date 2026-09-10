package com.bookflow.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

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

import com.bookflow.backend.appointment.AppointmentRepository;
import com.bookflow.backend.auth.AuthService;
import com.bookflow.backend.auth.dto.AuthResponse;
import com.bookflow.backend.auth.dto.RegisterRequest;
import com.bookflow.backend.availability.StaffAvailabilityExceptionRepository;
import com.bookflow.backend.availability.StaffWeeklyHoursRepository;
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
class AvailabilityBookingIntegrationTest {

	private static final String MONDAY = "2026-09-14";
	private static final String SLOT_09 = "2026-09-14T09:00:00Z";
	private static final String SLOT_10 = "2026-09-14T10:00:00Z";
	private static final String SLOT_13 = "2026-09-14T13:00:00Z";

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

	@Autowired
	private StaffWeeklyHoursRepository staffWeeklyHoursRepository;

	@Autowired
	private StaffAvailabilityExceptionRepository staffAvailabilityExceptionRepository;

	@BeforeEach
	@AfterEach
	void cleanDatabase() {
		appointmentRepository.deleteAllInBatch();
		staffAvailabilityExceptionRepository.deleteAllInBatch();
		staffWeeklyHoursRepository.deleteAllInBatch();
		staffRepository.deleteAllInBatch();
		serviceRepository.deleteAllInBatch();
		customerRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
		tenantRepository.deleteAllInBatch();
	}

	@Test
	void bookedSlotDisappearsFromAvailableSlotsAndCanBeReusedAfterCancel() throws Exception {
		BookingFixture fixture = createFixture(60);
		createWeeklyHours(fixture, "MONDAY", "09:00:00", "11:00:00");

		mockMvc.perform(get(slotsUrl(fixture, MONDAY, MONDAY))
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].startTime").value(SLOT_09))
			.andExpect(jsonPath("$[?(@.startTime=='" + SLOT_10 + "')]").isNotEmpty());

		long appointmentId = createAppointment(fixture, SLOT_09);

		mockMvc.perform(get(slotsUrl(fixture, MONDAY, MONDAY))
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.startTime=='" + SLOT_09 + "')]").isEmpty())
			.andExpect(jsonPath("$[?(@.startTime=='" + SLOT_10 + "')]").isNotEmpty());

		mockMvc.perform(patch("/api/appointments/{appointmentId}/status", appointmentId)
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "status": "CANCELLED"
						}
						"""))
			.andExpect(status().isOk());

		mockMvc.perform(get(slotsUrl(fixture, MONDAY, MONDAY))
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].startTime").value(SLOT_09));

		createAppointment(fixture, SLOT_09);
		assertEquals(2, appointmentRepository.count());
	}

	@Test
	void appointmentOutsideWeeklyHoursOrGridIsRejected() throws Exception {
		BookingFixture fixture = createFixture(60);
		createWeeklyHours(fixture, "MONDAY", "09:00:00", "12:00:00");

		mockMvc.perform(post("/api/appointments")
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(appointmentPayload(fixture, "2026-09-14T07:00:00Z")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("INVALID_OPERATION"));

		mockMvc.perform(post("/api/appointments")
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(appointmentPayload(fixture, "2026-09-14T09:07:00Z")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("INVALID_OPERATION"));

		mockMvc.perform(post("/api/appointments")
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(appointmentPayload(fixture, "2026-09-15T09:00:00Z")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("INVALID_OPERATION"));

		assertEquals(0, appointmentRepository.count());
	}

	@Test
	void allDayUnavailabilityBlocksSlotsAndBooking() throws Exception {
		BookingFixture fixture = createFixture(60);
		createWeeklyHours(fixture, "MONDAY", "09:00:00", "12:00:00");
		createException(fixture, """
				{
				  "exceptionDate": "%s",
				  "type": "UNAVAILABLE",
				  "note": "Holiday"
				}
				""".formatted(MONDAY));

		mockMvc.perform(get(slotsUrl(fixture, MONDAY, MONDAY))
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));

		mockMvc.perform(post("/api/appointments")
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(appointmentPayload(fixture, SLOT_09)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("INVALID_OPERATION"));
	}

	@Test
	void customHoursOverrideWeeklyHoursForCreateAndReschedule() throws Exception {
		BookingFixture fixture = createFixture(60);
		createWeeklyHours(fixture, "MONDAY", "09:00:00", "17:00:00");
		createException(fixture, """
				{
				  "exceptionDate": "%s",
				  "type": "CUSTOM_HOURS",
				  "startTime": "13:00:00",
				  "endTime": "15:00:00"
				}
				""".formatted(MONDAY));

		mockMvc.perform(post("/api/appointments")
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(appointmentPayload(fixture, SLOT_09)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("INVALID_OPERATION"));

		long appointmentId = createAppointment(fixture, SLOT_13);

		mockMvc.perform(put("/api/appointments/{appointmentId}", appointmentId)
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(appointmentPayload(fixture, SLOT_09)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("INVALID_OPERATION"));

		mockMvc.perform(put("/api/appointments/{appointmentId}", appointmentId)
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(appointmentPayload(fixture, "2026-09-14T13:30:00Z")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.startTime").value("2026-09-14T13:30:00Z"))
			.andExpect(jsonPath("$.endTime").value("2026-09-14T14:30:00Z"));
	}

	@Test
	void staffCanReadOwnSlotsButCannotChangeHoursOrViewAnotherStaff() throws Exception {
		BookingFixture fixture = createFixture(60);
		createWeeklyHours(fixture, "MONDAY", "09:00:00", "11:00:00");
		StaffIdentity ownStaff = createStaffIdentity(fixture.tenant(), "anna@example.com");
		createWeeklyHoursForStaff(fixture.ownerToken(), ownStaff.staff().getId(),
				"MONDAY", "09:00:00", "11:00:00");

		mockMvc.perform(get(slotsUrl(ownStaff.staff().getId(), fixture.service().getId(), MONDAY, MONDAY))
				.header(HttpHeaders.AUTHORIZATION, bearer(ownStaff.token())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].startTime").value(SLOT_09));

		mockMvc.perform(get(slotsUrl(fixture, MONDAY, MONDAY))
				.header(HttpHeaders.AUTHORIZATION, bearer(ownStaff.token())))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error").value("FORBIDDEN"));

		mockMvc.perform(post("/api/staff/{staffId}/availability/weekly-hours", fixture.staff().getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(ownStaff.token()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "dayOfWeek": "TUESDAY",
						  "startTime": "09:00:00",
						  "endTime": "12:00:00"
						}
						"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error").value("FORBIDDEN"));
	}

	@Test
	void availabilityEndpointsRemainTenantScoped() throws Exception {
		BookingFixture fixture = createFixture(60);
		createWeeklyHours(fixture, "MONDAY", "09:00:00", "11:00:00");
		AuthResponse otherOwner = authService.register(new RegisterRequest(
				"Other Studio",
				"other@example.com",
				null,
				"other-owner@example.com",
				"password123"));

		mockMvc.perform(get("/api/staff/{staffId}/availability/weekly-hours", fixture.staff().getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(otherOwner.accessToken())))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));

		mockMvc.perform(get(slotsUrl(fixture, MONDAY, MONDAY))
				.header(HttpHeaders.AUTHORIZATION, bearer(otherOwner.accessToken())))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
	}

	private void createWeeklyHours(
			BookingFixture fixture,
			String dayOfWeek,
			String startTime,
			String endTime) throws Exception {
		createWeeklyHoursForStaff(
				fixture.ownerToken(),
				fixture.staff().getId(),
				dayOfWeek,
				startTime,
				endTime);
	}

	private void createWeeklyHoursForStaff(
			String token,
			Long staffId,
			String dayOfWeek,
			String startTime,
			String endTime) throws Exception {
		mockMvc.perform(post("/api/staff/{staffId}/availability/weekly-hours", staffId)
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "dayOfWeek": "%s",
						  "startTime": "%s",
						  "endTime": "%s"
						}
						""".formatted(dayOfWeek, startTime, endTime)))
			.andExpect(status().isCreated());
	}

	private void createException(BookingFixture fixture, String payload) throws Exception {
		mockMvc.perform(post("/api/staff/{staffId}/availability/exceptions", fixture.staff().getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isCreated());
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

	private String slotsUrl(BookingFixture fixture, String from, String to) {
		return slotsUrl(fixture.staff().getId(), fixture.service().getId(), from, to);
	}

	private String slotsUrl(Long staffId, Long serviceId, String from, String to) {
		return "/api/staff/%d/availability/slots?serviceId=%d&from=%s&to=%s"
				.formatted(staffId, serviceId, from, to);
	}

	private String appointmentPayload(BookingFixture fixture, String startTime) {
		return """
				{
				  "customerId": %d,
				  "staffId": %d,
				  "serviceId": %d,
				  "startTime": "%s",
				  "notes": "Availability booking test"
				}
				""".formatted(
					fixture.customer().getId(),
					fixture.staff().getId(),
					fixture.service().getId(),
					startTime);
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
