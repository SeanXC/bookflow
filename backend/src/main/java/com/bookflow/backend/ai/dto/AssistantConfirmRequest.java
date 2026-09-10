package com.bookflow.backend.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Confirms a previously proposed booking. Do not send tenant IDs.")
public record AssistantConfirmRequest(@NotBlank String proposalId) {
}
