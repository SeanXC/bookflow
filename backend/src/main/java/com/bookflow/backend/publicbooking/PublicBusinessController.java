package com.bookflow.backend.publicbooking;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookflow.backend.common.error.ApiErrorResponse;
import com.bookflow.backend.publicbooking.dto.PublicBusinessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public/businesses")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Public booking", description = "Unauthenticated public booking APIs")
@ApiResponses({
	@ApiResponse(responseCode = "404", description = "Business not found or not publicly bookable",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class PublicBusinessController {

	private final PublicProfileService publicProfileService;

	@GetMapping("/{slug}")
	@Operation(summary = "Get a public business profile by booking slug")
	public PublicBusinessResponse getPublicBusiness(@PathVariable String slug) {
		return PublicBusinessResponse.from(publicProfileService.getPublicBusiness(slug));
	}
}
