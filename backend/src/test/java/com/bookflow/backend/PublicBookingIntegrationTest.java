package com.bookflow.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.bookflow.backend.appointment.AppointmentRepository;
import com.bookflow.backend.auth.AuthService;
import com.bookflow.backend.auth.dto.AuthResponse;
import com.bookflow.backend.auth.dto.RegisterRequest;
import com.bookflow.backend.availability.StaffWeeklyHours;
import com.bookflow.backend.availability.StaffWeeklyHoursRepository;
import com.bookflow.backend.customer.CustomerRepository;
import com.bookflow.backend.publicbooking.PublicBookingRateLimiter;
import com.bookflow.backend.service.ServiceRepository;
import com.bookflow.backend.staff.Staff;
import com.bookflow.backend.staff.StaffRepository;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;
import com.bookflow.backend.user.UserRepository;

@SpringBootTest(properties = {
	"bookflow.security.jwt.secret=test-only-secret-at-least-32-characters"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PublicBookingIntegrationTest {

	private static final String SLUG = "glow-studio";
	private static final String MONDAY = "2026-09-14";
	private static final String SLOT_09 = "2026-09-14T09:00:00Z";
	private static final String SLOT_10 = "2026-09-14T10:00:00Z";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthService authService;

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
	private PublicBookingRateLimiter publicBookingRateLimiter;

	@BeforeEach
	@AfterEach
	void cleanDatabase() {
		publicBookingRateLimiter.clear();
		appointmentRepository.deleteAllInBatch();
		staffWeeklyHoursRepository.deleteAllInBatch();
		staffRepository.deleteAllInBatch();
		serviceRepository.deleteAllInBatch();
		customerRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
		tenantRepository.deleteAllInBatch();
	}

	@Test
	void publicCatalogIsAvailableWithoutAuthentication() throws Exception {
		BookingFixture fixture = createFixture(60);
		Staff inactiveStaff = new Staff(
				fixture.tenant(),
				null,
				"Hidden",
				"Staff",
				null);
		inactiveStaff.deactivate();
		staffRepository.saveAndFlush(inactiveStaff);
		com.bookflow.backend.service.Service inactiveService =
				new com.bookflow.backend.service.Service(
						fixture.tenant(),
						"Hidden Colour",
						null,
						new BigDecimal("80.00"),
						60);
		inactiveService.deactivate();
		serviceRepository.saveAndFlush(inactiveService);

		mockMvc.perform(get("/api/public/businesses/{slug}", SLUG))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.slug").value(SLUG))
			.andExpect(jsonPath("$.name").value("Glow Studio"))
			.andExpect(jsonPath("$.publicBookingEnabled").doesNotExist());

		mockMvc.perform(get("/api/public/businesses/{slug}/services", SLUG))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].id").value(fixture.service().getId()))
			.andExpect(jsonPath("$[0].name").value("Haircut"))
			.andExpect(jsonPath("$[0].active").doesNotExist());

		mockMvc.perform(get("/api/public/businesses/{slug}/staff", SLUG))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].id").value(fixture.staff().getId()))
			.andExpect(jsonPath("$[0].firstName").value("Anna"))
			.andExpect(jsonPath("$[0].phone").doesNotExist());

		mockMvc.perform(get(publicSlotsUrl(fixture, MONDAY, MONDAY)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].startTime").value(SLOT_09))
			.andExpect(jsonPath("$[?(@.startTime=='" + SLOT_10 + "')]").isNotEmpty());

		mockMvc.perform(get(publicSlotsUrl(
				fixture.staff().getId(),
				inactiveService.getId(),
				MONDAY,
				MONDAY)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	void publicAppointmentCreatesACustomerWithoutLogin() throws Exception {
		BookingFixture fixture = createFixture(60);

		mockMvc.perform(post("/api/public/businesses/{slug}/appointments", SLUG)
				.contentType(MediaType.APPLICATION_JSON)
				.content(publicAppointmentPayload(fixture, SLOT_09, "emma@example.com")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.startTime").value(SLOT_09))
			.andExpect(jsonPath("$.endTime").value(SLOT_10))
			.andExpect(jsonPath("$.status").value("CONFIRMED"))
			.andExpect(jsonPath("$.customerFirstName").value("Emma"))
			.andExpect(jsonPath("$.staff.firstName").value("Anna"))
			.andExpect(jsonPath("$.service.name").value("Haircut"))
			.andExpect(jsonPath("$.customerId").doesNotExist());

		assertEquals(1, appointmentRepository.count());
		assertEquals(1, customerRepository.count());

		mockMvc.perform(get(publicSlotsUrl(fixture, MONDAY, MONDAY)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.startTime=='" + SLOT_09 + "')]").isEmpty())
			.andExpect(jsonPath("$[?(@.startTime=='" + SLOT_10 + "')]").isNotEmpty());

		mockMvc.perform(get("/api/appointments")
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].startTime").value(SLOT_09));
	}

	@Test
	void publicAppointmentReusesACustomerByEmailWithinTheTenant() throws Exception {
		BookingFixture fixture = createFixture(60);

		mockMvc.perform(post("/api/public/businesses/{slug}/appointments", SLUG)
				.contentType(MediaType.APPLICATION_JSON)
				.content(publicAppointmentPayload(fixture, SLOT_09, "emma@example.com")))
			.andExpect(status().isCreated());
		mockMvc.perform(post("/api/public/businesses/{slug}/appointments", SLUG)
				.contentType(MediaType.APPLICATION_JSON)
				.content(publicAppointmentPayload(fixture, SLOT_10, "Emma@example.com")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.customerFirstName").value("Emma"));

		assertEquals(2, appointmentRepository.count());
		assertEquals(1, customerRepository.count());
	}

	@Test
	void disabledPublicBookingAndUnknownSlugReturnNotFound() throws Exception {
		BookingFixture fixture = createFixture(60);

		mockMvc.perform(put("/api/public-profile")
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Glow Studio",
						  "slug": "glow-studio",
						  "publicBookingEnabled": false
						}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.publicBookingEnabled").value(false));

		mockMvc.perform(get("/api/public-profile")
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.publicBookingEnabled").value(false));

		mockMvc.perform(get("/api/public/businesses/{slug}", SLUG))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
		mockMvc.perform(get("/api/public/businesses/{slug}/services", SLUG))
			.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/public/businesses/{slug}/appointments", SLUG)
				.contentType(MediaType.APPLICATION_JSON)
				.content(publicAppointmentPayload(fixture, SLOT_09, "emma@example.com")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
		mockMvc.perform(get("/api/public/businesses/{slug}", "missing-salon"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));

		assertEquals(0, appointmentRepository.count());
	}

	@Test
	void publicBookingStaysTenantScoped() throws Exception {
		BookingFixture glow = createFixture(60);
		BookingFixture other = createOtherFixture();

		mockMvc.perform(get("/api/public/businesses/{slug}/staff", SLUG))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].id").value(glow.staff().getId()));

		mockMvc.perform(post("/api/public/businesses/{slug}/appointments", SLUG)
				.contentType(MediaType.APPLICATION_JSON)
				.content(publicAppointmentPayload(
						other.staff().getId(),
						glow.service().getId(),
						SLOT_09,
						"emma@example.com")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));

		mockMvc.perform(post("/api/public/businesses/{slug}/appointments", SLUG)
				.contentType(MediaType.APPLICATION_JSON)
				.content(publicAppointmentPayload(
						glow.staff().getId(),
						other.service().getId(),
						SLOT_09,
						"emma@example.com")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));

		mockMvc.perform(post("/api/public/businesses/{slug}/appointments", SLUG)
				.contentType(MediaType.APPLICATION_JSON)
				.content(publicAppointmentPayload(glow, SLOT_09, "shared@example.com")))
			.andExpect(status().isCreated());
		mockMvc.perform(post("/api/public/businesses/{slug}/appointments", other.tenant().getSlug())
				.contentType(MediaType.APPLICATION_JSON)
				.content(publicAppointmentPayload(other, SLOT_09, "shared@example.com")))
			.andExpect(status().isCreated());

		assertEquals(2, appointmentRepository.count());
		assertEquals(2, customerRepository.count());
	}

	@Test
	void publicBookingRejectsInactiveResourcesUnavailableSlotsAndConflicts() throws Exception {
		BookingFixture fixture = createFixture(60);

		mockMvc.perform(post("/api/public/businesses/{slug}/appointments", SLUG)
				.contentType(MediaType.APPLICATION_JSON)
				.content(publicAppointmentPayload(fixture, "2026-09-14T07:00:00Z", "emma@example.com")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("INVALID_OPERATION"));

		mockMvc.perform(post("/api/public/businesses/{slug}/appointments", SLUG)
				.contentType(MediaType.APPLICATION_JSON)
				.content(publicAppointmentPayload(fixture, SLOT_09, "emma@example.com")))
			.andExpect(status().isCreated());
		mockMvc.perform(post("/api/public/businesses/{slug}/appointments", SLUG)
				.contentType(MediaType.APPLICATION_JSON)
				.content(publicAppointmentPayload(fixture, SLOT_09, "other@example.com")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("BOOKING_CONFLICT"));

		fixture.staff().deactivate();
		staffRepository.saveAndFlush(fixture.staff());
		mockMvc.perform(post("/api/public/businesses/{slug}/appointments", SLUG)
				.contentType(MediaType.APPLICATION_JSON)
				.content(publicAppointmentPayload(fixture, SLOT_10, "late@example.com")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));

		assertEquals(1, appointmentRepository.count());
	}

	@Test
	void publicAppointmentCreateIsRateLimitedByIpAndSlug() throws Exception {
		createFixture(60);

		for (int attempt = 0; attempt < 5; attempt++) {
			mockMvc.perform(post("/api/public/businesses/{slug}/appointments", SLUG)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isBadRequest());
		}

		mockMvc.perform(post("/api/public/businesses/{slug}/appointments", SLUG)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isTooManyRequests())
			.andExpect(header().string("Retry-After", "600"))
			.andExpect(jsonPath("$.error").value("RATE_LIMITED"));

		mockMvc.perform(get("/api/public/businesses/{slug}", SLUG))
			.andExpect(status().isOk());
	}

	private BookingFixture createFixture(int durationMinutes) {
		AuthResponse owner = authService.register(new RegisterRequest(
				"Glow Studio",
				"hello@example.com",
				null,
				"owner@example.com",
				"password123"));
		Tenant tenant = tenantRepository.findAll().getFirst();
		return persistCatalog(owner.accessToken(), tenant, durationMinutes);
	}

	private BookingFixture createOtherFixture() {
		AuthResponse owner = authService.register(new RegisterRequest(
				"Other Studio",
				"other@example.com",
				null,
				"other-owner@example.com",
				"password123"));
		Tenant tenant = tenantRepository.findAll().stream()
				.filter(item -> "other-studio".equals(item.getSlug()))
				.findFirst()
				.orElseThrow();
		return persistCatalog(owner.accessToken(), tenant, 60);
	}

	private BookingFixture persistCatalog(
			String ownerToken,
			Tenant tenant,
			int durationMinutes) {
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
		staffWeeklyHoursRepository.saveAndFlush(new StaffWeeklyHours(
				tenant,
				staff,
				DayOfWeek.MONDAY,
				LocalTime.of(9, 0),
				LocalTime.of(12, 0)));
		return new BookingFixture(ownerToken, tenant, staff, service);
	}

	private String publicSlotsUrl(BookingFixture fixture, String from, String to) {
		return publicSlotsUrl(fixture.staff().getId(), fixture.service().getId(), from, to);
	}

	private String publicSlotsUrl(Long staffId, Long serviceId, String from, String to) {
		return "/api/public/businesses/%s/staff/%d/slots?serviceId=%d&from=%s&to=%s"
				.formatted(SLUG, staffId, serviceId, from, to);
	}

	private String publicAppointmentPayload(
			BookingFixture fixture,
			String startTime,
			String email) {
		return publicAppointmentPayload(
				fixture.staff().getId(),
				fixture.service().getId(),
				startTime,
				email);
	}

	private String publicAppointmentPayload(
			Long staffId,
			Long serviceId,
			String startTime,
			String email) {
		return """
				{
				  "staffId": %d,
				  "serviceId": %d,
				  "startTime": "%s",
				  "firstName": "Emma",
				  "lastName": "Smith",
				  "email": "%s",
				  "phone": "0871112222",
				  "notes": "Public booking test"
				}
				""".formatted(staffId, serviceId, startTime, email);
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}

	private record BookingFixture(
			String ownerToken,
			Tenant tenant,
			Staff staff,
			com.bookflow.backend.service.Service service) {
	}
}
