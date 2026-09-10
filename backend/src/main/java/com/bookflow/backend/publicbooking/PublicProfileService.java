package com.bookflow.backend.publicbooking;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookflow.backend.common.exception.DuplicateResourceException;
import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.publicbooking.dto.PublicProfileRequest;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicProfileService {

	private final TenantRepository tenantRepository;

	public Tenant getPublicBusiness(String slug) {
		return tenantRepository.findBySlug(slug)
				.filter(Tenant::isPublicBookingEnabled)
				.orElseThrow(() -> new ResourceNotFoundException("Business", slug));
	}

	@PreAuthorize("hasRole('OWNER')")
	public Tenant getPublicProfile(Long tenantId) {
		return getTenant(tenantId);
	}

	@PreAuthorize("hasRole('OWNER')")
	@Transactional
	public Tenant updatePublicProfile(Long tenantId, PublicProfileRequest request) {
		Tenant tenant = getTenant(tenantId);
		String slug = request.slug().trim();
		if (!tenant.getSlug().equals(slug) && tenantRepository.existsBySlug(slug)) {
			throw duplicateSlug();
		}

		tenant.updatePublicProfile(
				request.name().trim(),
				normalizeOptional(request.phone()),
				slug,
				normalizeOptional(request.description()),
				request.publicBookingEnabled());
		try {
			tenantRepository.flush();
			return tenant;
		} catch (DataIntegrityViolationException exception) {
			throw duplicateSlug();
		}
	}

	private Tenant getTenant(Long tenantId) {
		return tenantRepository.findById(tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
	}

	private DuplicateResourceException duplicateSlug() {
		return new DuplicateResourceException("This booking slug is already in use");
	}

	private String normalizeOptional(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
