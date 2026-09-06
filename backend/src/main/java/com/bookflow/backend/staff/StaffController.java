package com.bookflow.backend.staff;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookflow.backend.common.dto.PageResponse;
import com.bookflow.backend.common.error.ApiErrorResponse;
import com.bookflow.backend.security.CurrentUserProvider;
import com.bookflow.backend.staff.dto.StaffRequest;
import com.bookflow.backend.staff.dto.StaffResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@Tag(name = "Staff", description = "Tenant-scoped staff management")
@ApiResponses({
	@ApiResponse(responseCode = "400", description = "Invalid request",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "401", description = "Authentication required",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "403", description = "Insufficient role permission",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "404", description = "Staff or related resource not found",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class StaffController {

	private final StaffService staffService;
	private final CurrentUserProvider currentUserProvider;

	@GetMapping
	@Operation(summary = "List and search staff")
	public PageResponse<StaffResponse> getAllStaff(
			@Parameter(description = "Case-insensitive first, last, or full name search")
			@RequestParam(required = false) String search,
			@Parameter(description = "Filter by active or inactive status")
			@RequestParam(required = false) Boolean active,
			@PageableDefault(
				size = 20,
				sort = {"lastName", "firstName", "id"}) Pageable pageable) {
		return PageResponse.from(staffService.getAllStaff(
					currentUserProvider.getTenantId(),
					search,
					active,
					pageable)
				.map(StaffResponse::from));
	}

	@GetMapping("/{staffId}")
	@Operation(summary = "Get a staff member")
	public StaffResponse getStaff(@PathVariable Long staffId) {
		return StaffResponse.from(
				staffService.getStaff(currentUserProvider.getTenantId(), staffId));
	}

	@PostMapping
	@Operation(summary = "Create a staff member")
	public ResponseEntity<StaffResponse> createStaff(
			@Valid @RequestBody StaffRequest request) {
		Staff staff = staffService.createStaff(
				currentUserProvider.getTenantId(),
				request.userId(),
				request.firstName(),
				request.lastName(),
				request.phone());
		return ResponseEntity.status(HttpStatus.CREATED).body(StaffResponse.from(staff));
	}

	@PutMapping("/{staffId}")
	@Operation(summary = "Update a staff member")
	public StaffResponse updateStaff(
			@PathVariable Long staffId,
			@Valid @RequestBody StaffRequest request) {
		Staff staff = staffService.updateStaff(
				currentUserProvider.getTenantId(),
				staffId,
				request.userId(),
				request.firstName(),
				request.lastName(),
				request.phone());
		return StaffResponse.from(staff);
	}

	@DeleteMapping("/{staffId}")
	@Operation(summary = "Deactivate a staff member")
	public ResponseEntity<Void> deactivateStaff(@PathVariable Long staffId) {
		staffService.deactivateStaff(currentUserProvider.getTenantId(), staffId);
		return ResponseEntity.noContent().build();
	}
}
