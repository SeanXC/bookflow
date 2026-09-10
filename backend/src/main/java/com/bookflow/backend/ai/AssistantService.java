package com.bookflow.backend.ai;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.bookflow.backend.ai.dto.AssistantBookingResponse;
import com.bookflow.backend.ai.dto.AssistantChatRequest;
import com.bookflow.backend.ai.dto.AssistantChatResponse;
import com.bookflow.backend.ai.dto.AssistantConfirmRequest;
import com.bookflow.backend.ai.dto.AssistantMessageRequest;
import com.bookflow.backend.ai.dto.AssistantMessageRole;
import com.bookflow.backend.ai.dto.AssistantProposalResponse;
import com.bookflow.backend.common.exception.InvalidOperationException;
import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.publicbooking.PublicBookingService;
import com.bookflow.backend.publicbooking.PublicProfileService;
import com.bookflow.backend.publicbooking.dto.PublicAppointmentRequest;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
@RequiredArgsConstructor
public class AssistantService {

	static final int MAX_TOOL_ROUNDS = 6;

	private final LlmClient llmClient;
	private final BookingToolExecutor bookingToolExecutor;
	private final AssistantProposalStore proposalStore;
	private final PublicBookingService publicBookingService;
	private final PublicProfileService publicProfileService;
	private final TenantRepository tenantRepository;
	private final Clock businessClock;
	private final JsonMapper jsonMapper;

	public AssistantChatResponse chatForPublicSlug(String slug, AssistantChatRequest request) {
		return chat(publicProfileService.getPublicBusiness(slug), request);
	}

	@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST', 'STAFF')")
	public AssistantChatResponse chatForTenant(Long tenantId, AssistantChatRequest request) {
		return chat(requireTenant(tenantId), request);
	}

	public AssistantBookingResponse confirmForPublicSlug(
			String slug,
			AssistantConfirmRequest request) {
		return confirm(publicProfileService.getPublicBusiness(slug), request);
	}

	@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST', 'STAFF')")
	public AssistantBookingResponse confirmForTenant(
			Long tenantId,
			AssistantConfirmRequest request) {
		return confirm(requireTenant(tenantId), request);
	}

	private AssistantChatResponse chat(Tenant tenant, AssistantChatRequest request) {
		validateConversation(request);
		List<LlmMessage> messages = new ArrayList<>();
		messages.add(LlmMessage.system(AssistantPrompt.systemPrompt(
				tenant.getName(),
				businessClock.getZone(),
				LocalDate.now(businessClock))));
		for (AssistantMessageRequest message : request.messages()) {
			if (message.role() == AssistantMessageRole.USER) {
				messages.add(LlmMessage.user(message.content().trim()));
			} else {
				messages.add(LlmMessage.assistant(message.content().trim(), List.of()));
			}
		}

		AssistantProposalResponse proposal = null;
		for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
			LlmCompletion completion = llmClient.complete(
					new LlmRequest(messages, BookingToolContract.tools()));
			if (!completion.hasToolCalls()) {
				return new AssistantChatResponse(resolveReply(completion.content()), proposal);
			}

			messages.add(LlmMessage.assistant(completion.content(), completion.toolCalls()));
			int callIndex = 0;
			for (LlmToolCall toolCall : completion.toolCalls()) {
				String result = executeTool(tenant.getId(), toolCall);
				AssistantProposalResponse next = readProposal(tenant.getId(), toolCall.name(), result);
				if (next != null) {
					proposal = next;
				}
				messages.add(LlmMessage.tool(toolCallId(toolCall, callIndex++), toolCall.name(), result));
			}
		}
		throw new InvalidOperationException(
				"The booking assistant is temporarily unavailable.");
	}

	private AssistantBookingResponse confirm(Tenant tenant, AssistantConfirmRequest request) {
		if (request == null || request.proposalId() == null || request.proposalId().isBlank()) {
			throw new InvalidOperationException("This booking proposal is no longer valid");
		}
		String proposalId = request.proposalId().trim();
		AssistantProposalResponse proposal = proposalStore.consume(proposalId, tenant.getId());
		try {
			assertProposalStillBookable(tenant.getId(), proposal);
			return AssistantBookingResponse.from(publicBookingService.bookGuestAppointment(
					tenant,
					toAppointmentRequest(proposal)));
		} catch (RuntimeException exception) {
			proposalStore.restore(proposalId, tenant.getId(), proposal);
			throw exception;
		}
	}

	private void assertProposalStillBookable(Long tenantId, AssistantProposalResponse proposal) {
		if (!looksLikeEmail(proposal.email())) {
			throw new InvalidOperationException("A valid email is required to confirm this booking");
		}
		String result = executeTool(tenantId, new LlmToolCall(
				"confirm",
				BookingToolContract.PROPOSE_BOOKING,
				writeProposalArguments(proposal)));
		JsonNode node = jsonMapper.readTree(result);
		JsonNode error = node.get("error");
		if (error != null && !error.isNull() && !error.isMissingNode()) {
			throw new InvalidOperationException(error.asString());
		}
		if (AssistantProposalResponse.fromToolResult(node) == null) {
			throw new InvalidOperationException("This booking proposal is no longer valid");
		}
	}

	private PublicAppointmentRequest toAppointmentRequest(AssistantProposalResponse proposal) {
		return new PublicAppointmentRequest(
				proposal.staffId(),
				proposal.serviceId(),
				proposal.startTime(),
				proposal.firstName(),
				proposal.lastName(),
				proposal.email(),
				proposal.phone(),
				proposal.notes());
	}

	private String writeProposalArguments(AssistantProposalResponse proposal) {
		ObjectNode arguments = jsonMapper.createObjectNode();
		arguments.put("staffId", proposal.staffId());
		arguments.put("serviceId", proposal.serviceId());
		arguments.put("startTime", proposal.startTime().toString());
		arguments.put("firstName", proposal.firstName());
		arguments.put("lastName", proposal.lastName());
		arguments.put("email", proposal.email());
		arguments.put("phone", proposal.phone());
		if (proposal.notes() != null) {
			arguments.put("notes", proposal.notes());
		}
		return jsonMapper.writeValueAsString(arguments);
	}

	private boolean looksLikeEmail(String email) {
		return email != null && email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
	}

	private Tenant requireTenant(Long tenantId) {
		return tenantRepository.findById(tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
	}

	private void validateConversation(AssistantChatRequest request) {
		if (request == null || request.messages() == null || request.messages().isEmpty()) {
			throw new InvalidOperationException("A user message is required");
		}
		AssistantMessageRequest latest = request.messages().getLast();
		if (latest.role() != AssistantMessageRole.USER) {
			throw new InvalidOperationException("The latest message must come from the guest");
		}
	}

	private String executeTool(Long tenantId, LlmToolCall toolCall) {
		try {
			return bookingToolExecutor.execute(tenantId, toolCall);
		} catch (InvalidOperationException exception) {
			return writeError(exception.getMessage());
		}
	}

	private AssistantProposalResponse readProposal(
			Long tenantId,
			String toolName,
			String resultJson) {
		if (!BookingToolContract.PROPOSE_BOOKING.equals(toolName) || resultJson == null) {
			return null;
		}
		try {
			AssistantProposalResponse proposal = AssistantProposalResponse.fromToolResult(
					jsonMapper.readTree(resultJson));
			if (proposal == null) {
				return null;
			}
			return proposal.withProposalId(proposalStore.save(tenantId, proposal));
		} catch (RuntimeException exception) {
			return null;
		}
	}

	private String toolCallId(LlmToolCall toolCall, int index) {
		if (toolCall.id() == null || toolCall.id().isBlank()) {
			return "call_" + index;
		}
		return toolCall.id();
	}

	private String resolveReply(String content) {
		if (content == null || content.isBlank()) {
			return "I could not complete that request. Please try again.";
		}
		return content;
	}

	private String writeError(String message) {
		ObjectNode node = jsonMapper.createObjectNode();
		node.put("error", message);
		return jsonMapper.writeValueAsString(node);
	}
}
