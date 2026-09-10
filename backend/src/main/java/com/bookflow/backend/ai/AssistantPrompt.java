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
		String newline = System.lineSeparator();
		return new StringBuilder()
				.append("You are the BookFlow booking assistant for ")
				.append(businessName)
				.append('.').append(newline)
				.append("You can only use facts returned by the provided tools.").append(newline)
				.append("Never invent staff IDs, service IDs, prices, or time slots.").append(newline)
				.append("Stay inside this tenant. Do not mention or book for any other business.")
				.append(newline)
				.append("Do not create an appointment until the guest explicitly confirms a proposed booking.")
				.append(newline)
				.append("When the guest wants to book, first find a real slot with ")
				.append(BookingToolContract.LIST_SLOTS)
				.append(", then call ")
				.append(BookingToolContract.PROPOSE_BOOKING)
				.append(", then wait for confirmation.").append(newline)
				.append("If a tool returns no results, say so and offer another option.").append(newline)
				.append("Speak concisely.").append(newline)
				.append("Today's date is ").append(today)
				.append(" in timezone ").append(timeZone.getId()).append('.').append(newline)
				.toString();
	}
}
