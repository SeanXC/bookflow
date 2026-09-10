package com.bookflow.backend.ai.dto;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

@Schema(description = "A validated booking proposal that still requires guest confirmation")
public record AssistantProposalResponse(
		boolean requiresConfirmation,
		Long staffId,
		String staffFirstName,
		String staffLastName,
		Long serviceId,
		String serviceName,
		BigDecimal price,
		int durationMinutes,
		Instant startTime,
		Instant endTime,
		String firstName,
		String lastName,
		String email,
		String phone,
		String notes) {

	public static AssistantProposalResponse fromToolResult(JsonNode node) {
		if (node == null || !"proposed".equals(text(node, "status"))) {
			return null;
		}
		Long staffId = longValue(node, "staffId");
		Long serviceId = longValue(node, "serviceId");
		Instant startTime = instant(node, "startTime");
		Instant endTime = instant(node, "endTime");
		String firstName = text(node, "firstName");
		String lastName = text(node, "lastName");
		String email = text(node, "email");
		String phone = text(node, "phone");
		if (staffId == null
				|| serviceId == null
				|| startTime == null
				|| endTime == null
				|| firstName == null
				|| lastName == null
				|| email == null
				|| phone == null) {
			return null;
		}
		return new AssistantProposalResponse(
				true,
				staffId,
				text(node, "staffFirstName"),
				text(node, "staffLastName"),
				serviceId,
				text(node, "serviceName"),
				decimal(node, "price"),
				(int) node.path("durationMinutes").asLong(),
				startTime,
				endTime,
				firstName,
				lastName,
				email,
				phone,
				text(node, "notes"));
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull() || value.isMissingNode()) {
			return null;
		}
		String text = value.asString();
		return text == null || text.isBlank() ? null : text;
	}

	private static Long longValue(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull() || value.isMissingNode() || !value.isNumber()) {
			return null;
		}
		return value.asLong();
	}

	private static Instant instant(JsonNode node, String field) {
		String value = text(node, field);
		if (value == null) {
			return null;
		}
		try {
			return Instant.parse(value);
		} catch (RuntimeException exception) {
			return null;
		}
	}

	private static BigDecimal decimal(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull() || value.isMissingNode()) {
			return null;
		}
		try {
			return new BigDecimal(value.asString());
		} catch (RuntimeException exception) {
			return null;
		}
	}
}
