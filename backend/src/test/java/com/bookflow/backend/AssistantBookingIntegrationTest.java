package com.bookflow.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.bookflow.backend.ai.AssistantMockLlmConfiguration;
import com.bookflow.backend.ai.AssistantProposalStore;
import com.bookflow.backend.ai.AssistantRateLimiter;
import com.bookflow.backend.ai.ScriptedBookingLlmClient;
import com.bookflow.backend.appointment.AppointmentRepository;
import com.bookflow.backend.auth.AuthService;
import com.bookflow.backend.auth.dto.AuthResponse;
import com.bookflow.backend.auth.dto.RegisterRequest;
import com.bookflow.backend.availability.StaffWeeklyHours;
import com.bookflow.backend.availability.StaffWeeklyHoursRepository;
import com.bookflow.backend.customer.CustomerRepository;
import com.bookflow.backend.service.ServiceRepository;
import com.bookflow.backend.staff.Staff;
import com.bookflow.backend.staff.StaffRepository;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;
import com.bookflow.backend.user.UserRepository;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {
	"bookflow.security.jwt.secret=test-only-secret-at-least-32-characters",
	"bookflow.ai.llm.api-key=",
	"bookflow.ai.rate-limit.max-requests=3"
})
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, AssistantMockLlmConfiguration.class})
class AssistantBookingIntegrationTest {

	private static final String SLUG = "glow-studio";
	private static final String SLOT_09 = "2026-09-14T09:00:00Z";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private ScriptedBookingLlmClient scriptedBookingLlmClient;

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
	private AssistantRateLimiter assistantRateLimiter;

	@Autowired
	private AssistantProposalStore assistantProposalStore;

	@BeforeEach
	@AfterEach
	void cleanDatabase() {
		scriptedBookingLlmClient.reset();
		assistantRateLimiter.clear();
		assistantProposalStore.clear();
		appointmentRepository.deleteAllInBatch();
		staffWeeklyHoursRepository.deleteAllInBatch();
		staffRepository.deleteAllInBatch();
		serviceRepository.deleteAllInBatch();
		customerRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
		tenantRepository.deleteAllInBatch();
	}

	@Test
	void publicAssistantBooksOnlyAfterTheGuestConfirmsARealSlot() throws Exception {
		BookingFixture fixture = createFixture();

		String chatBody = mockMvc.perform(post("/api/public/businesses/{slug}/assistant", SLUG)
				.contentType(MediaType.APPLICATION_JSON)
				.content(userMessage("I want a haircut on Monday morning")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Please confirm your Haircut with Anna at 09:00."))
			.andExpect(jsonPath("$.proposal.requiresConfirmation").value(true))
			.andExpect(jsonPath("$.proposal.staffId").value(fixture.staff().getId()))
			.andExpect(jsonPath("$.proposal.serviceId").value(fixture.service().getId()))
			.andExpect(jsonPath("$.proposal.startTime").value(SLOT_09))
			.andExpect(jsonPath("$.proposal.proposalId").isNotEmpty())
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertEquals(0, appointmentRepository.count());
		assertEquals(0, customerRepository.count());

		String proposalId = jsonMapper.readTree(chatBody).path("proposal").path("proposalId").asString();
		mockMvc.perform(post("/api/public/businesses/{slug}/assistant/confirm", SLUG)
				.contentType(MediaType.APPLICATION_JSON)
				.content(confirmPayload(proposalId)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.appointmentId").isNumber())
			.andExpect(jsonPath("$.staffId").value(fixture.staff().getId()))
			.andExpect(jsonPath("$.serviceName").value("Haircut"))
			.andExpect(jsonPath("$.startTime").value(SLOT_09))
			.andExpect(jsonPath("$.status").value("CONFIRMED"))
			.andExpect(jsonPath("$.firstName").value("Emma"));

		assertEquals(1, appointmentRepository.count());
		assertEquals(1, customerRepository.count());
	}

	@Test
	void publicAssistantDoesNotBookAnInventedUnavailableSlot() throws Exception {
		createFixture();
		scriptedBookingLlmClient.useUnavailableSlot();

		mockMvc.perform(post("/api/public/businesses/{slug}/assistant", SLUG)
				.contentType(MediaType.APPLICATION_JSON)
				.content(userMessage("Book me at 7am")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(
					"That time is not available. Please choose another slot."))
			.andExpect(jsonPath("$.proposal.proposalId").doesNotExist());

		assertEquals(0, appointmentRepository.count());
	}

	@Test
	void publicAssistantProposalCannotBeConfirmedOnAnotherBusiness() throws Exception {
		createFixture();
		BookingFixture other = createOtherFixture();

		String chatBody = mockMvc.perform(post("/api/public/businesses/{slug}/assistant", SLUG)
				.contentType(MediaType.APPLICATION_JSON)
				.content(userMessage("Book a haircut")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.proposal.proposalId").isNotEmpty())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String proposalId = jsonMapper.readTree(chatBody).path("proposal").path("proposalId").asString();

		mockMvc.perform(post(
				"/api/public/businesses/{slug}/assistant/confirm",
				other.tenant().getSlug())
				.contentType(MediaType.APPLICATION_JSON)
				.content(confirmPayload(proposalId)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("INVALID_OPERATION"));

		assertEquals(0, appointmentRepository.count());

		mockMvc.perform(post("/api/public/businesses/{slug}/assistant/confirm", SLUG)
				.contentType(MediaType.APPLICATION_JSON)
				.content(confirmPayload(proposalId)))
			.andExpect(status().isCreated());
		assertEquals(1, appointmentRepository.count());
	}

	@Test
	void tenantAssistantUsesTheJwtTenantAndStillRequiresConfirmation() throws Exception {
		BookingFixture fixture = createFixture();

		String chatBody = mockMvc.perform(post("/api/assistant")
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(userMessage("Book Anna on Monday")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.proposal.staffId").value(fixture.staff().getId()))
			.andExpect(jsonPath("$.proposal.startTime").value(SLOT_09))
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertEquals(0, appointmentRepository.count());

		String proposalId = jsonMapper.readTree(chatBody).path("proposal").path("proposalId").asString();
		mockMvc.perform(post("/api/assistant/confirm")
				.header(HttpHeaders.AUTHORIZATION, bearer(fixture.ownerToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(confirmPayload(proposalId)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.serviceName").value("Haircut"));

		assertEquals(1, appointmentRepository.count());
	}

	@Test
	void publicAssistantIsHiddenWhenPublicBookingIsDisabled() throws Exception {
		BookingFixture fixture = createFixture();

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
			.andExpect(status().isOk());

		mockMvc.perform(post("/api/public/businesses/{slug}/assistant", SLUG)
				.contentType(MediaType.APPLICATION_JSON)
				.content(userMessage("Hi")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));

		assertEquals(0, scriptedBookingLlmClient.completionCount());
		assertEquals(0, appointmentRepository.count());
	}

	@Test
	void publicAssistantChatIsRateLimited() throws Exception {
		createFixture();

		for (int attempt = 0; attempt < 3; attempt++) {
			mockMvc.perform(post("/api/public/businesses/{slug}/assistant", SLUG)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isBadRequest());
		}

		mockMvc.perform(post("/api/public/businesses/{slug}/assistant", SLUG)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isTooManyRequests())
			.andExpect(header().string("Retry-After", "600"))
			.andExpect(jsonPath("$.error").value("RATE_LIMITED"));
	}

	@Test
	void tenantAssistantRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/assistant")
				.contentType(MediaType.APPLICATION_JSON)
				.content(userMessage("Hi")))
			.andExpect(status().isUnauthorized());
	}

	private BookingFixture createFixture() {
		AuthResponse owner = authService.register(new RegisterRequest(
				"Glow Studio",
				"hello@example.com",
				null,
				"owner@example.com",
				"password123"));
		Tenant tenant = tenantRepository.findAll().getFirst();
		return persistCatalog(owner.accessToken(), tenant);
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
		return persistCatalog(owner.accessToken(), tenant);
	}

	private BookingFixture persistCatalog(String ownerToken, Tenant tenant) {
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
						60));
		staffWeeklyHoursRepository.saveAndFlush(new StaffWeeklyHours(
				tenant,
				staff,
				DayOfWeek.MONDAY,
				LocalTime.of(9, 0),
				LocalTime.of(12, 0)));
		return new BookingFixture(ownerToken, tenant, staff, service);
	}

	private String userMessage(String content) {
		return """
				{
				  "messages": [
				    { "role": "USER", "content": "%s" }
				  ]
				}
				""".formatted(content);
	}

	private String confirmPayload(String proposalId) {
		return """
				{ "proposalId": "%s" }
				""".formatted(proposalId);
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
