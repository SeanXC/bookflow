package com.bookflow.backend.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Assistant reply for the current tenant. A proposal is not a booking.")
public record AssistantChatResponse(
		String message,
		AssistantProposalResponse proposal) {
}
