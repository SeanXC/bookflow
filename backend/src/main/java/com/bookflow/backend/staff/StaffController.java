package com.bookflow.backend.staff;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookflow.backend.security.CurrentUserProvider;
import com.bookflow.backend.staff.dto.StaffRequest;
import com.bookflow.backend.staff.dto.StaffResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

	private final StaffService staffService;
	private final CurrentUserProvider currentUserProvider;

	@GetMapping
	public Page<StaffResponse> getAllStaff(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Boolean active,
			Pageable pageable) {
		return staffService.getAllStaff(
					currentUserProvider.getTenantId(),
					search,
					active,
					pageable)
				.map(StaffResponse::from);
	}

	@GetMapping("/{staffId}")
	public StaffResponse getStaff(@PathVariable Long staffId) {
		return StaffResponse.from(
				staffService.getStaff(currentUserProvider.getTenantId(), staffId));
	}

	@PostMapping
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
	public ResponseEntity<Void> deactivateStaff(@PathVariable Long staffId) {
		staffService.deactivateStaff(currentUserProvider.getTenantId(), staffId);
		return ResponseEntity.noContent().build();
	}
}
