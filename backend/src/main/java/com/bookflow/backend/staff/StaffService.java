package com.bookflow.backend.staff;

import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookflow.backend.common.exception.DuplicateResourceException;
import com.bookflow.backend.common.exception.InvalidOperationException;
import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;
import com.bookflow.backend.user.Role;
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
		User user = getOptionalStaffUser(tenantId, userId, null);

		try {
			return staffRepository.saveAndFlush(
					new Staff(tenant, user, firstName, lastName, phone));
		} catch (DataIntegrityViolationException exception) {
			throw duplicateStaffAccountLink();
		}
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
		User user = getOptionalStaffUser(tenantId, userId, staffId);
		staff.updateDetails(user, firstName, lastName, phone);
		try {
			staffRepository.flush();
		} catch (DataIntegrityViolationException exception) {
			throw duplicateStaffAccountLink();
		}
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

	private User getOptionalStaffUser(
			Long tenantId,
			Long userId,
			Long currentStaffId) {
		if (userId == null) {
			return null;
		}

		User user = userRepository.findByIdAndTenantId(userId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("User", userId));
		if (user.getRole() != Role.STAFF) {
			throw new InvalidOperationException(
					"Only STAFF user accounts can be linked to staff records");
		}

		staffRepository.findByUserIdAndTenantId(userId, tenantId)
				.filter(linkedStaff -> !Objects.equals(linkedStaff.getId(), currentStaffId))
				.ifPresent(linkedStaff -> {
					throw duplicateStaffAccountLink();
				});
		return user;
	}

	private DuplicateResourceException duplicateStaffAccountLink() {
		return new DuplicateResourceException(
				"This user account is already linked to a staff member");
	}

	private String escapeLikePattern(String value) {
		return value
				.replace("!", "!!")
				.replace("%", "!%")
				.replace("_", "!_");
	}
}
