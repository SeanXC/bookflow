package com.bookflow.backend.publicbooking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bookflow.backend.common.exception.DuplicateResourceException;
import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.publicbooking.dto.PublicProfileRequest;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;

@ExtendWith(MockitoExtension.class)
class PublicProfileServiceTest {

	private static final Long TENANT_ID = 10L;

	@Mock
	private TenantRepository tenantRepository;

	private PublicProfileService publicProfileService;

	@BeforeEach
	void setUp() {
		publicProfileService = new PublicProfileService(tenantRepository);
	}

	@Test
	void getPublicBusinessHidesDisabledProfiles() {
		Tenant tenant = new Tenant("Glow Studio", "hello@example.com", null);
		tenant.updatePublicProfile(
				tenant.getName(),
				tenant.getPhone(),
				tenant.getSlug(),
				null,
				false);
		when(tenantRepository.findBySlug("glow-studio")).thenReturn(Optional.of(tenant));

		assertThrows(
				ResourceNotFoundException.class,
				() -> publicProfileService.getPublicBusiness("glow-studio"));
	}

	@Test
	void updatePublicProfileRejectsATakenSlug() {
		Tenant tenant = new Tenant("Glow Studio", "hello@example.com", null);
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		when(tenantRepository.existsBySlug("other-salon")).thenReturn(true);

		assertThrows(
				DuplicateResourceException.class,
				() -> publicProfileService.updatePublicProfile(
						TENANT_ID,
						new PublicProfileRequest(
								"Glow Studio",
								null,
								"other-salon",
								"Evening appointments",
								true)));
	}

	@Test
	void updatePublicProfileSavesDisplayFields() {
		Tenant tenant = new Tenant("Glow Studio", "hello@example.com", null);
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));

		Tenant updated = publicProfileService.updatePublicProfile(
				TENANT_ID,
				new PublicProfileRequest(
						" Glow Rooms ",
						" 0871234567 ",
						"glow-rooms",
						"  Colour and cuts  ",
						false));

		assertEquals("Glow Rooms", updated.getName());
		assertEquals("0871234567", updated.getPhone());
		assertEquals("glow-rooms", updated.getSlug());
		assertEquals("Colour and cuts", updated.getDescription());
		assertFalse(updated.isPublicBookingEnabled());
		verify(tenantRepository).flush();
	}
}
