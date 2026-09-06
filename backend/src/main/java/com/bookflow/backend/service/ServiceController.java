package com.bookflow.backend.service;

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
import com.bookflow.backend.service.dto.ServiceRequest;
import com.bookflow.backend.service.dto.ServiceResponse;

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
@RequestMapping("/api/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "Tenant-scoped bookable service management")
@ApiResponses({
	@ApiResponse(responseCode = "400", description = "Invalid request",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "401", description = "Authentication required",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "403", description = "Insufficient role permission",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "404", description = "Service not found",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class ServiceController {

	private final ServiceManagementService serviceManagementService;
	private final CurrentUserProvider currentUserProvider;

	@GetMapping
	@Operation(summary = "List and search services")
	public PageResponse<ServiceResponse> getAllServices(
			@Parameter(description = "Case-insensitive service name or description search")
			@RequestParam(required = false) String search,
			@Parameter(description = "Filter by active or inactive status")
			@RequestParam(required = false) Boolean active,
			@PageableDefault(
				size = 20,
				sort = {"name", "id"}) Pageable pageable) {
		return PageResponse.from(serviceManagementService
				.getAllServices(
						currentUserProvider.getTenantId(),
						search,
						active,
						pageable)
				.map(ServiceResponse::from));
	}

	@GetMapping("/{serviceId}")
	@Operation(summary = "Get a service")
	public ServiceResponse getService(@PathVariable Long serviceId) {
		return ServiceResponse.from(serviceManagementService.getService(
				currentUserProvider.getTenantId(),
				serviceId));
	}

	@PostMapping
	@Operation(summary = "Create a service")
	public ResponseEntity<ServiceResponse> createService(
			@Valid @RequestBody ServiceRequest request) {
		Service service = serviceManagementService.createService(
				currentUserProvider.getTenantId(),
				request.name(),
				request.description(),
				request.price(),
				request.durationMinutes());
		return ResponseEntity.status(HttpStatus.CREATED).body(ServiceResponse.from(service));
	}

	@PutMapping("/{serviceId}")
	@Operation(summary = "Update a service")
	public ServiceResponse updateService(
			@PathVariable Long serviceId,
			@Valid @RequestBody ServiceRequest request) {
		Service service = serviceManagementService.updateService(
				currentUserProvider.getTenantId(),
				serviceId,
				request.name(),
				request.description(),
				request.price(),
				request.durationMinutes());
		return ServiceResponse.from(service);
	}

	@DeleteMapping("/{serviceId}")
	@Operation(summary = "Deactivate a service")
	public ResponseEntity<Void> deactivateService(@PathVariable Long serviceId) {
		serviceManagementService.deactivateService(
				currentUserProvider.getTenantId(),
				serviceId);
		return ResponseEntity.noContent().build();
	}
}
