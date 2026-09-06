package com.bookflow.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;

@ExtendWith(MockitoExtension.class)
class ServiceManagementServiceTest {

	private static final Long TENANT_ID = 10L;
	private static final Long SERVICE_ID = 20L;

	@Mock
	private ServiceRepository serviceRepository;

	@Mock
	private TenantRepository tenantRepository;

	private ServiceManagementService serviceManagementService;

	@BeforeEach
	void setUp() {
		serviceManagementService = new ServiceManagementService(
				serviceRepository,
				tenantRepository);
	}

	@Test
	void createServiceUsesTheAuthenticatedTenant() {
		Tenant tenant = tenant();
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		when(serviceRepository.save(any(Service.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Service service = serviceManagementService.createService(
				TENANT_ID,
				"Haircut",
				"Cut and style",
				new BigDecimal("30.00"),
				30);

		assertSame(tenant, service.getTenant());
		assertEquals("Haircut", service.getName());
		assertEquals("Cut and style", service.getDescription());
		assertEquals(new BigDecimal("30.00"), service.getPrice());
		assertEquals(30, service.getDurationMinutes());
	}

	@Test
	void createServiceRejectsAnUnknownTenant() {
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

		assertThrows(
				ResourceNotFoundException.class,
				() -> serviceManagementService.createService(
						TENANT_ID,
						"Haircut",
						null,
						new BigDecimal("30.00"),
						30));

		verify(serviceRepository, never()).save(any(Service.class));
	}

	@Test
	void updateServiceUsesTenantScopedLookupAndChangesDetails() {
		Service service = service();
		when(serviceRepository.findByIdAndTenantId(SERVICE_ID, TENANT_ID))
				.thenReturn(Optional.of(service));

		Service updated = serviceManagementService.updateService(
				TENANT_ID,
				SERVICE_ID,
				"Hair Colour",
				"Full colour",
				new BigDecimal("70.00"),
				90);

		assertSame(service, updated);
		assertEquals("Hair Colour", updated.getName());
		assertEquals("Full colour", updated.getDescription());
		assertEquals(new BigDecimal("70.00"), updated.getPrice());
		assertEquals(90, updated.getDurationMinutes());
		verify(serviceRepository).findByIdAndTenantId(SERVICE_ID, TENANT_ID);
	}

	@Test
	void getServiceRejectsCrossTenantAccessAsNotFound() {
		when(serviceRepository.findByIdAndTenantId(SERVICE_ID, TENANT_ID))
				.thenReturn(Optional.empty());

		assertThrows(
				ResourceNotFoundException.class,
				() -> serviceManagementService.getService(TENANT_ID, SERVICE_ID));
	}

	@Test
	void deactivateServiceUsesTenantScopedLookup() {
		Service service = service();
		when(serviceRepository.findByIdAndTenantId(SERVICE_ID, TENANT_ID))
				.thenReturn(Optional.of(service));

		serviceManagementService.deactivateService(TENANT_ID, SERVICE_ID);

		assertFalse(service.isActive());
		verify(serviceRepository).findByIdAndTenantId(SERVICE_ID, TENANT_ID);
	}

	@Test
	void searchServicesCombinesEscapedTextAndActiveFilter() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<Service> expected = Page.empty(pageable);
		when(serviceRepository.searchByTenantId(
				TENANT_ID,
				"Hair!%!_!!",
				true,
				pageable))
				.thenReturn(expected);

		Page<Service> result = serviceManagementService.getAllServices(
				TENANT_ID,
				"  Hair%_!  ",
				true,
				pageable);

		assertSame(expected, result);
	}

	@Test
	void activeFilterWithoutSearchUsesTheTenantAndActiveQuery() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<Service> expected = Page.empty(pageable);
		when(serviceRepository.findAllByTenantIdAndActive(
				TENANT_ID,
				false,
				pageable))
				.thenReturn(expected);

		Page<Service> result = serviceManagementService.getAllServices(
				TENANT_ID,
				null,
				false,
				pageable);

		assertSame(expected, result);
		verify(serviceRepository, never()).searchByTenantId(any(), any(), any(), any());
	}

	@Test
	void listWithoutFiltersUsesTheTenantListQuery() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<Service> expected = Page.empty(pageable);
		when(serviceRepository.findAllByTenantId(TENANT_ID, pageable))
				.thenReturn(expected);

		Page<Service> result = serviceManagementService.getAllServices(
				TENANT_ID,
				"  ",
				null,
				pageable);

		assertSame(expected, result);
		verify(serviceRepository, never()).searchByTenantId(any(), any(), any(), any());
		verify(serviceRepository, never()).findAllByTenantIdAndActive(
				any(),
				anyBoolean(),
				any());
	}

	private Tenant tenant() {
		return new Tenant("Glow Studio", "hello@example.com", null);
	}

	private Service service() {
		return new Service(
				tenant(),
				"Haircut",
				"Cut and style",
				new BigDecimal("30.00"),
				30);
	}
}
