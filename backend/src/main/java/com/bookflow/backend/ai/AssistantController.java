package com.bookflow.backend.ai;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookflow.backend.ai.dto.AssistantBookingResponse;
import com.bookflow.backend.ai.dto.AssistantChatRequest;
import com.bookflow.backend.ai.dto.AssistantChatResponse;
import com.bookflow.backend.ai.dto.AssistantConfirmRequest;
import com.bookflow.backend.common.error.ApiErrorResponse;
import com.bookflow.backend.security.CurrentUserProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
@Tag(name = "Assistant", description = "Tenant-scoped booking assistant")
@ApiResponses({
	@ApiResponse(responseCode = "400", description = "Invalid conversation or assistant is unavailable",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "401", description = "Authentication required",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "403", description = "Insufficient role permission",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "409", description = "Staff booking conflict",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "429", description = "Assistant rate limit exceeded",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class AssistantController {

	private final AssistantService assistantService;
	private final CurrentUserProvider currentUserProvider;

	@PostMapping
	@Operation(summary = "Chat with the booking assistant for the current tenant")
	public AssistantChatResponse chat(@Valid @RequestBody AssistantChatRequest request) {
		return assistantService.chatForTenant(currentUserProvider.getTenantId(), request);
	}

	@PostMapping("/confirm")
	@Operation(summary = "Confirm a proposed booking for the current tenant")
	public ResponseEntity<AssistantBookingResponse> confirm(
			@Valid @RequestBody AssistantConfirmRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(assistantService.confirmForTenant(
						currentUserProvider.getTenantId(),
						request));
	}
}
