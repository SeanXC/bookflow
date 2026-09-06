package com.bookflow.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookflow.backend.security.CurrentUserProvider;
import com.bookflow.backend.service.dto.ServiceRequest;
import com.bookflow.backend.service.dto.ServiceResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

	private final ServiceManagementService serviceManagementService;
	private final CurrentUserProvider currentUserProvider;

	@GetMapping
	public Page<ServiceResponse> getAllServices(Pageable pageable) {
		return serviceManagementService
				.getAllServices(currentUserProvider.getTenantId(), pageable)
				.map(ServiceResponse::from);
	}

	@GetMapping("/{serviceId}")
	public ServiceResponse getService(@PathVariable Long serviceId) {
		return ServiceResponse.from(serviceManagementService.getService(
				currentUserProvider.getTenantId(),
				serviceId));
	}

	@PostMapping
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
	public ResponseEntity<Void> deactivateService(@PathVariable Long serviceId) {
		serviceManagementService.deactivateService(
				currentUserProvider.getTenantId(),
				serviceId);
		return ResponseEntity.noContent().build();
	}
}
