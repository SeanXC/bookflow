package com.bookflow.backend.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class AssistantPromptTest {

	@Test
	void systemPromptBindsTheTenantAndRequiresConfirmation() {
		String prompt = AssistantPrompt.systemPrompt(
				"Glow Studio",
				ZoneId.of("UTC"),
				LocalDate.of(2026, 9, 14));

		assertTrue(prompt.contains("Glow Studio"));
		assertTrue(prompt.contains("2026-09-14"));
		assertTrue(prompt.contains("UTC"));
		assertTrue(prompt.contains(BookingToolContract.LIST_SLOTS));
		assertTrue(prompt.contains(BookingToolContract.PROPOSE_BOOKING));
		assertTrue(prompt.contains("explicitly confirms"));
		assertTrue(prompt.contains("Never invent"));
		assertTrue(prompt.contains("Stay inside this tenant"));
	}
}
