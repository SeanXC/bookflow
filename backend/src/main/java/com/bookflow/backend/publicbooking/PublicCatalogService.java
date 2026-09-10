package com.bookflow.backend.publicbooking;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookflow.backend.availability.AvailabilityService;
import com.bookflow.backend.availability.AvailableSlot;
import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.service.ServiceRepository;
import com.bookflow.backend.staff.Staff;
import com.bookflow.backend.staff.StaffRepository;
import com.bookflow.backend.tenant.Tenant;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicCatalogService {

	private final PublicProfileService publicProfileService;
	private final ServiceRepository serviceRepository;
	private final StaffRepository staffRepository;
	private final AvailabilityService availabilityService;

	public List<com.bookflow.backend.service.Service> getPublicServices(String slug) {
		Tenant tenant = publicProfileService.getPublicBusiness(slug);
		return serviceRepository.findAllByTenantIdAndActiveOrderByNameAscIdAsc(
				tenant.getId(),
				true);
	}

	public List<Staff> getPublicStaff(String slug) {
		Tenant tenant = publicProfileService.getPublicBusiness(slug);
		return staffRepository.findAllByTenantIdAndActiveOrderByLastNameAscFirstNameAscIdAsc(
				tenant.getId(),
				true);
	}

	public List<AvailableSlot> getPublicSlots(
			String slug,
			Long staffId,
			Long serviceId,
			LocalDate fromDate,
			LocalDate toDate) {
		Tenant tenant = publicProfileService.getPublicBusiness(slug);
		Staff staff = staffRepository.findByIdAndTenantId(staffId, tenant.getId())
				.filter(Staff::isActive)
				.orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));
		com.bookflow.backend.service.Service service = serviceRepository
				.findByIdAndTenantId(serviceId, tenant.getId())
				.filter(com.bookflow.backend.service.Service::isActive)
				.orElseThrow(() -> new ResourceNotFoundException("Service", serviceId));
		return availabilityService.calculateSlots(
				tenant.getId(),
				staff,
				service,
				fromDate,
				toDate);
	}
}
