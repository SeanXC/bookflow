package com.bookflow.backend.ai;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Deterministic LLM double for tests. It never opens a network connection.
 */
public class ScriptedBookingLlmClient implements LlmClient {

	public enum Mode {
		HAPPY_PATH,
		UNAVAILABLE_SLOT
	}

	private static final String SLOT_DATE = "2026-09-14";
	private static final String UNAVAILABLE_START = "2026-09-14T07:00:00Z";

	private final JsonMapper jsonMapper;
	private final AtomicInteger completions = new AtomicInteger();
	private volatile Mode mode = Mode.HAPPY_PATH;

	public ScriptedBookingLlmClient(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	public void reset() {
		mode = Mode.HAPPY_PATH;
		completions.set(0);
	}

	public void useUnavailableSlot() {
		mode = Mode.UNAVAILABLE_SLOT;
	}

	public int completionCount() {
		return completions.get();
	}

	@Override
	public LlmCompletion complete(LlmRequest request) {
		completions.incrementAndGet();
		String services = lastToolResult(request, BookingToolContract.LIST_SERVICES);
		if (services == null) {
			return toolCall("call_services", BookingToolContract.LIST_SERVICES, "{}");
		}
		if (isErrorOrEmpty(services)) {
			return text("There are no bookable services.");
		}

		String staff = lastToolResult(request, BookingToolContract.LIST_STAFF);
		if (staff == null) {
			return toolCall("call_staff", BookingToolContract.LIST_STAFF, "{}");
		}
		if (isErrorOrEmpty(staff)) {
			return text("There are no bookable staff members.");
		}

		if (mode == Mode.UNAVAILABLE_SLOT) {
			String proposed = lastToolResult(request, BookingToolContract.PROPOSE_BOOKING);
			if (proposed == null) {
				return toolCall(
						"call_propose",
						BookingToolContract.PROPOSE_BOOKING,
						proposeArguments(staff, services, UNAVAILABLE_START));
			}
			return text("That time is not available. Please choose another slot.");
		}

		String slots = lastToolResult(request, BookingToolContract.LIST_SLOTS);
		if (slots == null) {
			return toolCall(
					"call_slots",
					BookingToolContract.LIST_SLOTS,
					slotArguments(staff, services));
		}
		if (isErrorOrEmpty(slots)) {
			return text("I could not find an available slot.");
		}

		String proposed = lastToolResult(request, BookingToolContract.PROPOSE_BOOKING);
		if (proposed == null) {
			return toolCall(
					"call_propose",
					BookingToolContract.PROPOSE_BOOKING,
					proposeArguments(staff, services, firstSlotStart(slots)));
		}
		if (isError(proposed)) {
			return text("I could not propose that booking.");
		}
		return text("Please confirm your Haircut with Anna at 09:00.");
	}

	private String lastToolResult(LlmRequest request, String toolName) {
		for (int index = request.messages().size() - 1; index >= 0; index--) {
			LlmMessage message = request.messages().get(index);
			if (message.role() == LlmRole.TOOL && toolName.equals(message.name())) {
				return message.content();
			}
		}
		return null;
	}

	private String slotArguments(String staffJson, String servicesJson) {
		return """
				{"staffId":%d,"serviceId":%d,"from":"%s","to":"%s"}
				""".formatted(firstId(staffJson), firstId(servicesJson), SLOT_DATE, SLOT_DATE);
	}

	private String proposeArguments(String staffJson, String servicesJson, String startTime) {
		return """
				{
				  "staffId": %d,
				  "serviceId": %d,
				  "startTime": "%s",
				  "firstName": "Emma",
				  "lastName": "Smith",
				  "email": "emma@example.com",
				  "phone": "0871112222"
				}
				""".formatted(firstId(staffJson), firstId(servicesJson), startTime);
	}

	private long firstId(String json) {
		return firstElement(json).path("id").asLong();
	}

	private String firstSlotStart(String json) {
		return firstElement(json).path("startTime").asString();
	}

	private JsonNode firstElement(String json) {
		JsonNode node = jsonMapper.readTree(json);
		if (!node.isArray() || node.isEmpty()) {
			throw new IllegalStateException("Expected a non-empty tool result array");
		}
		return node.get(0);
	}

	private boolean isErrorOrEmpty(String json) {
		return isError(json) || isEmptyArray(json);
	}

	private boolean isError(String json) {
		JsonNode error = jsonMapper.readTree(json).get("error");
		return error != null && !error.isNull() && !error.isMissingNode();
	}

	private boolean isEmptyArray(String json) {
		JsonNode node = jsonMapper.readTree(json);
		return node.isArray() && node.isEmpty();
	}

	private LlmCompletion toolCall(String id, String name, String argumentsJson) {
		return new LlmCompletion(
				null,
				List.of(new LlmToolCall(id, name, argumentsJson)),
				"tool_calls");
	}

	private LlmCompletion text(String content) {
		return new LlmCompletion(content, List.of(), "stop");
	}
}
