package com.bookflow.backend.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;

import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceManagementService {

	private final ServiceRepository serviceRepository;
	private final TenantRepository tenantRepository;

	public Service getService(Long tenantId, Long serviceId) {
		return serviceRepository.findByIdAndTenantId(serviceId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Service", serviceId));
	}

	public Page<Service> getAllServices(Long tenantId, Pageable pageable) {
		return serviceRepository.findAllByTenantId(tenantId, pageable);
	}

	@Transactional
	public Service createService(
			Long tenantId,
			String name,
			String description,
			BigDecimal price,
			int durationMinutes) {
		Tenant tenant = tenantRepository.findById(tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

		return serviceRepository.save(
				new Service(tenant, name, description, price, durationMinutes));
	}

	@Transactional
	public Service updateService(
			Long tenantId,
			Long serviceId,
			String name,
			String description,
			BigDecimal price,
			int durationMinutes) {
		Service service = getService(tenantId, serviceId);
		service.updateDetails(name, description, price, durationMinutes);
		return service;
	}

	@Transactional
	public void deactivateService(Long tenantId, Long serviceId) {
		getService(tenantId, serviceId).deactivate();
	}
}
