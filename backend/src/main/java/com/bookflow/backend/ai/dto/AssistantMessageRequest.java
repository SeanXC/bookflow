package com.bookflow.backend.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "A guest or assistant text turn. System and tool messages are not accepted.")
public record AssistantMessageRequest(
		@NotNull AssistantMessageRole role,
		@NotBlank @Size(max = 2000) String content) {
}
