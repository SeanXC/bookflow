package com.bookflow.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookflow.backend.auth.dto.AuthResponse;
import com.bookflow.backend.auth.dto.LoginRequest;
import com.bookflow.backend.auth.dto.RegisterRequest;
import com.bookflow.backend.common.error.ApiErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Authentication", description = "Business registration and user login")
@ApiResponses({
	@ApiResponse(responseCode = "400", description = "Invalid request",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "401", description = "Invalid login credentials",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "409", description = "Email is already registered",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class AuthController {

	private final AuthService authService;

	@PostMapping("/register")
	@Operation(summary = "Register a business and its Owner account")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
	}

	@PostMapping("/login")
	@Operation(summary = "Authenticate a user and issue an access token")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}
}
