package com.bookflow.backend.publicbooking;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookflow.backend.common.error.ApiErrorResponse;
import com.bookflow.backend.publicbooking.dto.PublicProfileRequest;
import com.bookflow.backend.publicbooking.dto.PublicProfileResponse;
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
@RequestMapping("/api/public-profile")
@RequiredArgsConstructor
@Tag(name = "Public profile", description = "Owner-managed public booking profile")
@ApiResponses({
	@ApiResponse(responseCode = "400", description = "Invalid public profile",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "401", description = "Authentication required",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "403", description = "Owner role required",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "409", description = "Booking slug is already in use",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class PublicProfileController {

	private final PublicProfileService publicProfileService;
	private final CurrentUserProvider currentUserProvider;

	@GetMapping
	@Operation(summary = "Get the current tenant public booking profile")
	public PublicProfileResponse getPublicProfile() {
		return PublicProfileResponse.from(
				publicProfileService.getPublicProfile(currentUserProvider.getTenantId()));
	}

	@PutMapping
	@Operation(summary = "Update the current tenant public booking profile")
	public PublicProfileResponse updatePublicProfile(
			@Valid @RequestBody PublicProfileRequest request) {
		return PublicProfileResponse.from(publicProfileService.updatePublicProfile(
				currentUserProvider.getTenantId(),
				request));
	}
}
