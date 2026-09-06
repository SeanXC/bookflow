package com.bookflow.backend.staff;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;
import com.bookflow.backend.user.User;
import com.bookflow.backend.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffService {

	private final StaffRepository staffRepository;
	private final TenantRepository tenantRepository;
	private final UserRepository userRepository;

	@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST')")
	public Staff getStaff(Long tenantId, Long staffId) {
		return staffRepository.findByIdAndTenantId(staffId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));
	}

	@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST')")
	public Page<Staff> getAllStaff(
			Long tenantId,
			String search,
			Boolean active,
			Pageable pageable) {
		if (search != null && !search.isBlank()) {
			return staffRepository.searchByTenantId(
					tenantId,
					escapeLikePattern(search.trim()),
					active,
					pageable);
		}
		if (active != null) {
			return staffRepository.findAllByTenantIdAndActive(
					tenantId,
					active,
					pageable);
		}
		return staffRepository.findAllByTenantId(tenantId, pageable);
	}

	@PreAuthorize("hasRole('OWNER')")
	@Transactional
	public Staff createStaff(
			Long tenantId,
			Long userId,
			String firstName,
			String lastName,
			String phone) {
		Tenant tenant = getTenant(tenantId);
		User user = getOptionalUser(tenantId, userId);

		return staffRepository.save(new Staff(tenant, user, firstName, lastName, phone));
	}

	@PreAuthorize("hasRole('OWNER')")
	@Transactional
	public Staff updateStaff(
			Long tenantId,
			Long staffId,
			Long userId,
			String firstName,
			String lastName,
			String phone) {
		Staff staff = getStaff(tenantId, staffId);
		User user = getOptionalUser(tenantId, userId);
		staff.updateDetails(user, firstName, lastName, phone);
		return staff;
	}

	@PreAuthorize("hasRole('OWNER')")
	@Transactional
	public void deactivateStaff(Long tenantId, Long staffId) {
		getStaff(tenantId, staffId).deactivate();
	}

	private Tenant getTenant(Long tenantId) {
		return tenantRepository.findById(tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
	}

	private User getOptionalUser(Long tenantId, Long userId) {
		if (userId == null) {
			return null;
		}

		return userRepository.findByIdAndTenantId(userId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("User", userId));
	}

	private String escapeLikePattern(String value) {
		return value
				.replace("!", "!!")
				.replace("%", "!%")
				.replace("_", "!_");
	}
}
