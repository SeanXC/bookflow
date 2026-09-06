package com.bookflow.backend.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookflow.backend.common.error.ApiErrorResponse;
import com.bookflow.backend.security.CurrentUserProvider;
import com.bookflow.backend.user.dto.UserEnabledRequest;
import com.bookflow.backend.user.dto.UserRequest;
import com.bookflow.backend.user.dto.UserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Owner-only Receptionist and Staff account management")
@ApiResponses({
	@ApiResponse(responseCode = "400", description = "Invalid request or role",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "401", description = "Authentication required",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "403", description = "Owner role required",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "404", description = "User not found",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "409", description = "Email is already registered",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class UserController {

	private final UserManagementService userManagementService;
	private final CurrentUserProvider currentUserProvider;

	@GetMapping
	@Operation(summary = "List tenant user accounts")
	public Page<UserResponse> getAllUsers(
			@PageableDefault(size = 20, sort = {"email", "id"}) Pageable pageable) {
		return userManagementService
				.getAllUsers(currentUserProvider.getTenantId(), pageable)
				.map(UserResponse::from);
	}

	@PostMapping
	@Operation(summary = "Create a Receptionist or Staff account")
	public ResponseEntity<UserResponse> createUser(
			@Valid @RequestBody UserRequest request) {
		User user = userManagementService.createUser(
				currentUserProvider.getTenantId(),
				request.email(),
				request.password(),
				request.role());
		return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
	}

	@PatchMapping("/{userId}/enabled")
	@Operation(summary = "Enable or disable a Receptionist or Staff account")
	public UserResponse updateEnabled(
			@PathVariable Long userId,
			@Valid @RequestBody UserEnabledRequest request) {
		return UserResponse.from(userManagementService.updateEnabled(
				currentUserProvider.getTenantId(),
				userId,
				request.enabled()));
	}
}
