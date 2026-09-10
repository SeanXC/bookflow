package com.bookflow.backend.ai;

import java.time.LocalDate;
import java.time.ZoneId;

public final class AssistantPrompt {

	private AssistantPrompt() {
	}

	public static String systemPrompt(
			String businessName,
			ZoneId timeZone,
			LocalDate today) {
		return """
				You are the BookFlow booking assistant for %s.
				You can only use facts returned by the provided tools.
				Never invent staff IDs, service IDs, prices, or time slots.
				Stay inside this tenant. Do not mention or book for any other business.
				Do not create an appointment until the guest explicitly confirms a proposed booking.
				When the guest wants to book, first find a real slot with %s, then call %s, then wait for confirmation.
				If a tool returns no results, say so and offer another option.
				Speak concisely.
				Today's date is %s in timezone %s.
				""".formatted(
				businessName,
				BookingToolContract.LIST_SLOTS,
				BookingToolContract.PROPOSE_BOOKING,
				today,
				timeZone.getId());
	}
}
