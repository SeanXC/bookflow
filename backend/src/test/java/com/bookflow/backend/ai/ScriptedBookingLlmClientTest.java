package com.bookflow.backend.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class ScriptedBookingLlmClientTest {

	private ScriptedBookingLlmClient client;
	private List<LlmMessage> messages;

	@BeforeEach
	void setUp() {
		client = new ScriptedBookingLlmClient(JsonMapper.builder().build());
		messages = new ArrayList<>();
		messages.add(LlmMessage.user("Book a haircut"));
	}

	@Test
	void walksTheHappyPathFromCatalogToARealSlotProposal() {
		assertEquals(BookingToolContract.LIST_SERVICES, nextToolName());
		addToolResult(BookingToolContract.LIST_SERVICES, "[{\"id\":50,\"name\":\"Haircut\"}]");

		assertEquals(BookingToolContract.LIST_STAFF, nextToolName());
		addToolResult(BookingToolContract.LIST_STAFF, "[{\"id\":40,\"firstName\":\"Anna\"}]");

		LlmCompletion slots = client.complete(request());
		assertEquals(BookingToolContract.LIST_SLOTS, slots.toolCalls().getFirst().name());
		assertTrue(slots.toolCalls().getFirst().argumentsJson().contains("\"staffId\":40"));
		assertTrue(slots.toolCalls().getFirst().argumentsJson().contains("\"serviceId\":50"));
		addToolResult(
				BookingToolContract.LIST_SLOTS,
				"[{\"startTime\":\"2026-09-14T09:00:00Z\"}]");

		LlmCompletion propose = client.complete(request());
		assertEquals(BookingToolContract.PROPOSE_BOOKING, propose.toolCalls().getFirst().name());
		assertTrue(propose.toolCalls().getFirst().argumentsJson().contains("2026-09-14T09:00:00Z"));
		addToolResult(BookingToolContract.PROPOSE_BOOKING, "{\"status\":\"proposed\"}");

		assertEquals(
				"Please confirm your Haircut with Anna at 09:00.",
				client.complete(request()).content());
		assertEquals(5, client.completionCount());
	}

	@Test
	void refusesToTreatAnUnavailableInventedSlotAsBookable() {
		client.useUnavailableSlot();
		addToolResult(BookingToolContract.LIST_SERVICES, "[{\"id\":50}]");
		addToolResult(BookingToolContract.LIST_STAFF, "[{\"id\":40}]");

		LlmCompletion propose = client.complete(request());
		assertTrue(propose.toolCalls().getFirst().argumentsJson().contains("2026-09-14T07:00:00Z"));
		addToolResult(
				BookingToolContract.PROPOSE_BOOKING,
				"{\"error\":\"The requested start time is not an available booking slot\"}");

		assertEquals(
				"That time is not available. Please choose another slot.",
				client.complete(request()).content());
	}

	private String nextToolName() {
		return client.complete(request()).toolCalls().getFirst().name();
	}

	private void addToolResult(String toolName, String content) {
		messages.add(LlmMessage.tool("call_" + toolName, toolName, content));
	}

	private LlmRequest request() {
		return new LlmRequest(messages, BookingToolContract.tools());
	}
}
