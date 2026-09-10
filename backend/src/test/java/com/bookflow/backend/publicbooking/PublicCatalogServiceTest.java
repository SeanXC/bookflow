package com.bookflow.backend.publicbooking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bookflow.backend.availability.AvailabilityService;
import com.bookflow.backend.availability.AvailableSlot;
import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.service.ServiceRepository;
import com.bookflow.backend.staff.Staff;
import com.bookflow.backend.staff.StaffRepository;
import com.bookflow.backend.tenant.Tenant;

@ExtendWith(MockitoExtension.class)
class PublicCatalogServiceTest {

	private static final String SLUG = "glow-studio";
	private static final Long TENANT_ID = 10L;
	private static final Long STAFF_ID = 40L;
	private static final Long SERVICE_ID = 50L;

	@Mock
	private PublicProfileService publicProfileService;

	@Mock
	private ServiceRepository serviceRepository;

	@Mock
	private StaffRepository staffRepository;

	@Mock
	private AvailabilityService availabilityService;

	private PublicCatalogService publicCatalogService;
	private Tenant tenant;

	@BeforeEach
	void setUp() {
		publicCatalogService = new PublicCatalogService(
				publicProfileService,
				serviceRepository,
				staffRepository,
				availabilityService);
		tenant = new Tenant("Glow Studio", "hello@example.com", null);
		ReflectionTestUtils.setField(tenant, "id", TENANT_ID);
	}

	@Test
	void getPublicServicesUsesTheResolvedTenantAndActiveFilter() {
		com.bookflow.backend.service.Service service = haircut();
		when(publicProfileService.getPublicBusiness(SLUG)).thenReturn(tenant);
		when(serviceRepository.findAllByTenantIdAndActiveOrderByNameAscIdAsc(TENANT_ID, true))
				.thenReturn(List.of(service));

		assertEquals(List.of(service), publicCatalogService.getPublicServices(SLUG));
	}

	@Test
	void getPublicSlotsRejectsInactiveStaffAndServices() {
		Staff staff = new Staff(tenant, null, "Anna", "Smith", null);
		staff.deactivate();
		when(publicProfileService.getPublicBusiness(SLUG)).thenReturn(tenant);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));

		assertThrows(
				ResourceNotFoundException.class,
				() -> publicCatalogService.getPublicSlots(
						SLUG,
						STAFF_ID,
						SERVICE_ID,
						LocalDate.of(2026, 9, 14),
						LocalDate.of(2026, 9, 14)));

		verifyNoInteractions(availabilityService);
	}

	@Test
	void getPublicSlotsDelegatesToAvailabilityCalculation() {
		Staff staff = new Staff(tenant, null, "Anna", "Smith", null);
		com.bookflow.backend.service.Service service = haircut();
		List<AvailableSlot> slots = List.of(new AvailableSlot(
				Instant.parse("2026-09-14T09:00:00Z"),
				Instant.parse("2026-09-14T10:00:00Z")));
		when(publicProfileService.getPublicBusiness(SLUG)).thenReturn(tenant);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(serviceRepository.findByIdAndTenantId(SERVICE_ID, TENANT_ID))
				.thenReturn(Optional.of(service));
		when(availabilityService.calculateSlots(
				TENANT_ID,
				staff,
				service,
				LocalDate.of(2026, 9, 14),
				LocalDate.of(2026, 9, 14)))
				.thenReturn(slots);

		assertEquals(
				slots,
				publicCatalogService.getPublicSlots(
						SLUG,
						STAFF_ID,
						SERVICE_ID,
						LocalDate.of(2026, 9, 14),
						LocalDate.of(2026, 9, 14)));
		verify(availabilityService).calculateSlots(
				TENANT_ID,
				staff,
				service,
				LocalDate.of(2026, 9, 14),
				LocalDate.of(2026, 9, 14));
	}

	private com.bookflow.backend.service.Service haircut() {
		return new com.bookflow.backend.service.Service(
				tenant,
				"Haircut",
				null,
				new BigDecimal("30.00"),
				60);
	}
}
