package com.bookflow.backend.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bookflow.backend.ai.dto.AssistantBookingResponse;
import com.bookflow.backend.ai.dto.AssistantChatRequest;
import com.bookflow.backend.ai.dto.AssistantChatResponse;
import com.bookflow.backend.ai.dto.AssistantConfirmRequest;
import com.bookflow.backend.ai.dto.AssistantMessageRequest;
import com.bookflow.backend.ai.dto.AssistantMessageRole;
import com.bookflow.backend.appointment.Appointment;
import com.bookflow.backend.common.exception.InvalidOperationException;
import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.customer.Customer;
import com.bookflow.backend.publicbooking.PublicBookingService;
import com.bookflow.backend.publicbooking.PublicProfileService;
import com.bookflow.backend.publicbooking.dto.PublicAppointmentRequest;
import com.bookflow.backend.staff.Staff;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

	private static final String SLUG = "glow-studio";
	private static final Long TENANT_ID = 10L;
	private static final Long OTHER_TENANT_ID = 99L;

	@Mock
	private LlmClient llmClient;

	@Mock
	private BookingToolExecutor bookingToolExecutor;

	@Mock
	private PublicBookingService publicBookingService;

	@Mock
	private PublicProfileService publicProfileService;

	@Mock
	private TenantRepository tenantRepository;

	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private final Clock clock = Clock.fixed(
			Instant.parse("2026-09-14T12:00:00Z"),
			ZoneId.of("UTC"));
	private AssistantProposalStore proposalStore;
	private AssistantService assistantService;
	private Tenant tenant;

	@BeforeEach
	void setUp() {
		proposalStore = new AssistantProposalStore(
				new AssistantProperties(Duration.ofMinutes(10)),
				clock);
		assistantService = new AssistantService(
				llmClient,
				bookingToolExecutor,
				proposalStore,
				publicBookingService,
				publicProfileService,
				tenantRepository,
				clock,
				jsonMapper);
		tenant = new Tenant("Glow Studio", "hello@example.com", null);
		ReflectionTestUtils.setField(tenant, "id", TENANT_ID);
	}

	@Test
	void chatForPublicSlugUsesTheResolvedTenantAndReturnsAssistantText() {
		when(publicProfileService.getPublicBusiness(SLUG)).thenReturn(tenant);
		when(llmClient.complete(any())).thenReturn(textCompletion("We have haircuts available."));

		AssistantChatResponse response = assistantService.chatForPublicSlug(SLUG, userTurn(
				"Do you have haircuts?"));

		assertEquals("We have haircuts available.", response.message());
		assertNull(response.proposal());
		verifyNoInteractions(bookingToolExecutor, publicBookingService);
		verify(publicProfileService).getPublicBusiness(SLUG);
		verify(tenantRepository, never()).findById(any());
	}

	@Test
	void chatForTenantInjectsTheSystemPromptAndNeverReadsClientTenantIds() {
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		when(llmClient.complete(any())).thenReturn(textCompletion("I can help you book."));

		assistantService.chatForTenant(TENANT_ID, userTurn("Book a haircut"));

		LlmRequest request = captureLlmRequest();
		assertEquals(LlmRole.SYSTEM, request.messages().getFirst().role());
		assertTrue(request.messages().getFirst().content().contains("Glow Studio"));
		assertTrue(request.messages().getFirst().content().contains("2026-09-14"));
		assertEquals(
				BookingToolContract.tools().size(),
				request.tools().size());
		assertEquals("Book a haircut", request.messages().getLast().content());
		assertTrue(request.messages().stream().noneMatch(message ->
				message.content() != null && message.content().contains(OTHER_TENANT_ID.toString())));
	}

	@Test
	void chatExecutesToolsAgainstTheResolvedTenantThenContinues() {
		when(publicProfileService.getPublicBusiness(SLUG)).thenReturn(tenant);
		when(llmClient.complete(any())).thenReturn(
				toolCompletion("call_1", BookingToolContract.LIST_SERVICES, "{}"),
				textCompletion("Haircut is 60 minutes."));
		when(bookingToolExecutor.execute(eq(TENANT_ID), any(LlmToolCall.class)))
				.thenReturn("[{\"id\":50,\"name\":\"Haircut\"}]");

		AssistantChatResponse response = assistantService.chatForPublicSlug(SLUG, userTurn(
				"What services do you have?"));

		assertEquals("Haircut is 60 minutes.", response.message());
		ArgumentCaptor<LlmToolCall> toolCall = ArgumentCaptor.forClass(LlmToolCall.class);
		verify(bookingToolExecutor).execute(eq(TENANT_ID), toolCall.capture());
		assertEquals(BookingToolContract.LIST_SERVICES, toolCall.getValue().name());
		verify(bookingToolExecutor, never()).execute(eq(OTHER_TENANT_ID), any());
	}

	@Test
	void chatSurfacesAValidatedProposalWithoutTreatingItAsABooking() {
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		when(llmClient.complete(any())).thenReturn(
				toolCompletion(
						"call_2",
						BookingToolContract.PROPOSE_BOOKING,
						"""
						{"staffId":40,"serviceId":50,"startTime":"2026-09-14T09:00:00Z"}
						"""),
				textCompletion("Please confirm this haircut with Anna."));
		when(bookingToolExecutor.execute(eq(TENANT_ID), any(LlmToolCall.class)))
				.thenReturn(proposedJson());

		AssistantChatResponse response = assistantService.chatForTenant(
				TENANT_ID,
				userTurn("Book Anna tomorrow at 9"));

		assertEquals("Please confirm this haircut with Anna.", response.message());
		assertEquals(40L, response.proposal().staffId());
		assertEquals("Haircut", response.proposal().serviceName());
		assertTrue(response.proposal().requiresConfirmation());
		assertNotNull(response.proposal().proposalId());
		assertEquals(Instant.parse("2026-09-14T09:00:00Z"), response.proposal().startTime());
		verifyNoInteractions(publicBookingService);
	}

	@Test
	void chatForPublicSlugDoesNotCallTheLlmWhenTheBusinessIsHidden() {
		when(publicProfileService.getPublicBusiness(SLUG))
				.thenThrow(new ResourceNotFoundException("Business", SLUG));

		assertThrows(
				ResourceNotFoundException.class,
				() -> assistantService.chatForPublicSlug(SLUG, userTurn("Hi")));

		verifyNoInteractions(llmClient, bookingToolExecutor, publicBookingService);
	}

	@Test
	void chatForTenantRejectsAnUnknownTenantWithoutCallingTheLlm() {
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

		assertThrows(
				ResourceNotFoundException.class,
				() -> assistantService.chatForTenant(TENANT_ID, userTurn("Hi")));

		verifyNoInteractions(llmClient, bookingToolExecutor, publicBookingService, publicProfileService);
	}

	@Test
	void chatRejectsAConversationThatDoesNotEndWithTheGuest() {
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));

		assertThrows(
				InvalidOperationException.class,
				() -> assistantService.chatForTenant(
						TENANT_ID,
						new AssistantChatRequest(List.of(
								new AssistantMessageRequest(
										AssistantMessageRole.USER,
										"Hi"),
								new AssistantMessageRequest(
										AssistantMessageRole.ASSISTANT,
										"Hello")))));

		verifyNoInteractions(llmClient, bookingToolExecutor, publicBookingService);
	}

	@Test
	void chatFeedsUnknownToolFailuresBackAsToolErrors() {
		when(publicProfileService.getPublicBusiness(SLUG)).thenReturn(tenant);
		when(llmClient.complete(any())).thenReturn(
				toolCompletion("call_3", "delete_tenant", "{}"),
				textCompletion("I can only help with bookings."));
		when(bookingToolExecutor.execute(eq(TENANT_ID), any(LlmToolCall.class)))
				.thenThrow(new InvalidOperationException("Unknown booking tool: delete_tenant"));

		AssistantChatResponse response = assistantService.chatForPublicSlug(SLUG, userTurn(
				"Delete the other salon"));

		assertEquals("I can only help with bookings.", response.message());
		LlmRequest secondRequest = captureLlmRequest(1);
		LlmMessage toolMessage = secondRequest.messages().getLast();
		assertEquals(LlmRole.TOOL, toolMessage.role());
		assertTrue(toolMessage.content().contains("Unknown booking tool: delete_tenant"));
	}

	@Test
	void confirmBooksOnlyAfterAStoredProposalForTheSameTenant() {
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		when(llmClient.complete(any())).thenReturn(
				toolCompletion(
						"call_2",
						BookingToolContract.PROPOSE_BOOKING,
						"{}"),
				textCompletion("Please confirm this haircut with Anna."));
		when(bookingToolExecutor.execute(eq(TENANT_ID), any(LlmToolCall.class)))
				.thenReturn(proposedJson());
		when(publicBookingService.bookGuestAppointment(eq(tenant), any()))
				.thenReturn(bookedHaircut());

		AssistantChatResponse chat = assistantService.chatForTenant(
				TENANT_ID,
				userTurn("Book Anna tomorrow at 9"));
		AssistantBookingResponse booked = assistantService.confirmForTenant(
				TENANT_ID,
				new AssistantConfirmRequest(chat.proposal().proposalId()));

		assertEquals(99L, booked.appointmentId());
		assertEquals(40L, booked.staffId());
		ArgumentCaptor<PublicAppointmentRequest> request = ArgumentCaptor.forClass(
				PublicAppointmentRequest.class);
		verify(publicBookingService).bookGuestAppointment(eq(tenant), request.capture());
		assertEquals(40L, request.getValue().staffId());
		assertEquals(50L, request.getValue().serviceId());
		assertEquals("emma@example.com", request.getValue().email());
	}

	@Test
	void confirmRejectsAProposalFromAnotherTenant() {
		Tenant other = new Tenant("Other Salon", "other@example.com", null);
		ReflectionTestUtils.setField(other, "id", OTHER_TENANT_ID);
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		when(tenantRepository.findById(OTHER_TENANT_ID)).thenReturn(Optional.of(other));
		when(llmClient.complete(any())).thenReturn(
				toolCompletion(
						"call_2",
						BookingToolContract.PROPOSE_BOOKING,
						"{}"),
				textCompletion("Please confirm this haircut with Anna."));
		when(bookingToolExecutor.execute(eq(TENANT_ID), any(LlmToolCall.class)))
				.thenReturn(proposedJson());

		String proposalId = assistantService.chatForTenant(
				TENANT_ID,
				userTurn("Book Anna tomorrow at 9"))
				.proposal()
				.proposalId();

		assertThrows(
				InvalidOperationException.class,
				() -> assistantService.confirmForTenant(
						OTHER_TENANT_ID,
						new AssistantConfirmRequest(proposalId)));
		verify(publicBookingService, never()).bookGuestAppointment(any(), any());
		verify(bookingToolExecutor, never()).execute(eq(OTHER_TENANT_ID), any());

		when(publicBookingService.bookGuestAppointment(eq(tenant), any()))
				.thenReturn(bookedHaircut());
		AssistantBookingResponse booked = assistantService.confirmForTenant(
				TENANT_ID,
				new AssistantConfirmRequest(proposalId));
		assertEquals(99L, booked.appointmentId());
	}

	@Test
	void confirmRejectsAnUnknownProposalWithoutBooking() {
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));

		assertThrows(
				InvalidOperationException.class,
				() -> assistantService.confirmForTenant(
						TENANT_ID,
						new AssistantConfirmRequest("missing-proposal")));

		verifyNoInteractions(publicBookingService, bookingToolExecutor);
	}

	@Test
	void confirmDoesNotBookWhenTheSlotIsNoLongerAvailable() {
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		when(llmClient.complete(any())).thenReturn(
				toolCompletion(
						"call_2",
						BookingToolContract.PROPOSE_BOOKING,
						"{}"),
				textCompletion("Please confirm this haircut with Anna."));
		when(bookingToolExecutor.execute(eq(TENANT_ID), any(LlmToolCall.class)))
				.thenReturn(
						proposedJson(),
						"""
						{"error":"The requested start time is not an available booking slot"}
						""");

		String proposalId = assistantService.chatForTenant(
				TENANT_ID,
				userTurn("Book Anna tomorrow at 9"))
				.proposal()
				.proposalId();

		InvalidOperationException exception = assertThrows(
				InvalidOperationException.class,
				() -> assistantService.confirmForTenant(
						TENANT_ID,
						new AssistantConfirmRequest(proposalId)));

		assertEquals(
				"The requested start time is not an available booking slot",
				exception.getMessage());
		verify(publicBookingService, never()).bookGuestAppointment(any(), any());
	}

	private LlmRequest captureLlmRequest() {
		return captureLlmRequest(0);
	}

	private LlmRequest captureLlmRequest(int index) {
		ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
		verify(llmClient, atLeastOnce()).complete(captor.capture());
		return captor.getAllValues().get(index);
	}

	private AssistantChatRequest userTurn(String content) {
		return new AssistantChatRequest(List.of(
				new AssistantMessageRequest(AssistantMessageRole.USER, content)));
	}

	private LlmCompletion textCompletion(String content) {
		return new LlmCompletion(content, List.of(), "stop");
	}

	private LlmCompletion toolCompletion(String id, String name, String argumentsJson) {
		return new LlmCompletion(
				null,
				List.of(new LlmToolCall(id, name, argumentsJson)),
				"tool_calls");
	}

	private String proposedJson() {
		return """
				{
				  "status": "proposed",
				  "requiresConfirmation": true,
				  "staffId": 40,
				  "staffFirstName": "Anna",
				  "staffLastName": "Smith",
				  "serviceId": 50,
				  "serviceName": "Haircut",
				  "price": 30.00,
				  "durationMinutes": 60,
				  "startTime": "2026-09-14T09:00:00Z",
				  "endTime": "2026-09-14T10:00:00Z",
				  "firstName": "Emma",
				  "lastName": "Chen",
				  "email": "emma@example.com",
				  "phone": "555-0100"
				}
				""";
	}

	private Appointment bookedHaircut() {
		Customer customer = new Customer(
				tenant,
				"Emma",
				"Chen",
				"emma@example.com",
				"555-0100",
				null);
		ReflectionTestUtils.setField(customer, "id", 30L);
		Staff staff = new Staff(tenant, null, "Anna", "Smith", null);
		ReflectionTestUtils.setField(staff, "id", 40L);
		com.bookflow.backend.service.Service service = new com.bookflow.backend.service.Service(
				tenant,
				"Haircut",
				null,
				new BigDecimal("30.00"),
				60);
		ReflectionTestUtils.setField(service, "id", 50L);
		Appointment appointment = new Appointment(
				tenant,
				customer,
				staff,
				service,
				Instant.parse("2026-09-14T09:00:00Z"),
				Instant.parse("2026-09-14T10:00:00Z"),
				null);
		ReflectionTestUtils.setField(appointment, "id", 99L);
		return appointment;
	}
}
