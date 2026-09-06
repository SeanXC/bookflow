package com.bookflow.backend.user;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookflow.backend.common.exception.DuplicateResourceException;
import com.bookflow.backend.common.exception.InvalidOperationException;
import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasRole('OWNER')")
public class UserManagementService {

	private final UserRepository userRepository;
	private final TenantRepository tenantRepository;
	private final PasswordEncoder passwordEncoder;

	public Page<User> getAllUsers(Long tenantId, Pageable pageable) {
		return userRepository.findAllByTenantId(tenantId, pageable);
	}

	@Transactional
	public User createUser(
			Long tenantId,
			String email,
			String password,
			Role role) {
		validateManagedRole(role);
		String normalizedEmail = normalizeEmail(email);
		if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
			throw new DuplicateResourceException("A user with this email already exists");
		}

		Tenant tenant = tenantRepository.findById(tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
		try {
			return userRepository.saveAndFlush(new User(
					tenant,
					normalizedEmail,
					passwordEncoder.encode(password),
					role));
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateResourceException("A user with this email already exists");
		}
	}

	@Transactional
	public User updateEnabled(Long tenantId, Long userId, boolean enabled) {
		User user = userRepository.findByIdAndTenantId(userId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("User", userId));
		validateManagedRole(user.getRole());
		user.updateEnabled(enabled);
		return user;
	}

	private void validateManagedRole(Role role) {
		if (role != Role.RECEPTIONIST && role != Role.STAFF) {
			throw new InvalidOperationException(
					"Only RECEPTIONIST and STAFF accounts can be managed");
		}
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
