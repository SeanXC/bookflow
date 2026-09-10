package com.bookflow.backend.ai.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@Schema(description = "Tenant-scoped booking assistant conversation. Do not send tenant IDs.")
public record AssistantChatRequest(
		@NotEmpty
		@Size(max = 20)
		List<@Valid AssistantMessageRequest> messages) {
}
