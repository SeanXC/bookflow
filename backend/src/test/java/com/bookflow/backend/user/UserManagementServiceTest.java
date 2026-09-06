package com.bookflow.backend.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bookflow.backend.common.exception.DuplicateResourceException;
import com.bookflow.backend.common.exception.InvalidOperationException;
import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

	private static final Long TENANT_ID = 10L;
	private static final Long USER_ID = 20L;

	@Mock
	private UserRepository userRepository;

	@Mock
	private TenantRepository tenantRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	private UserManagementService userManagementService;

	@BeforeEach
	void setUp() {
		userManagementService = new UserManagementService(
				userRepository,
				tenantRepository,
				passwordEncoder);
	}

	@Test
	void listUsersUsesTheAuthenticatedTenant() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<User> expected = Page.empty(pageable);
		when(userRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(expected);

		Page<User> result = userManagementService.getAllUsers(TENANT_ID, pageable);

		assertSame(expected, result);
	}

	@Test
	void createReceptionistNormalizesEmailAndHashesPassword() {
		Tenant tenant = tenant();
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
		when(userRepository.saveAndFlush(any(User.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		User user = userManagementService.createUser(
				TENANT_ID,
				"  RECEPTION@EXAMPLE.COM  ",
				"password123",
				Role.RECEPTIONIST);

		assertSame(tenant, user.getTenant());
		assertEquals("reception@example.com", user.getEmail());
		assertEquals("encoded-password", user.getPasswordHash());
		assertEquals(Role.RECEPTIONIST, user.getRole());
		verify(userRepository).existsByEmailIgnoreCase("reception@example.com");
	}

	@Test
	void createStaffAccountUsesStaffRole() {
		Tenant tenant = tenant();
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
		when(userRepository.saveAndFlush(any(User.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		User user = userManagementService.createUser(
				TENANT_ID,
				"staff@example.com",
				"password123",
				Role.STAFF);

		assertEquals(Role.STAFF, user.getRole());
	}

	@Test
	void createUserRejectsOwnerRole() {
		assertThrows(
				InvalidOperationException.class,
				() -> userManagementService.createUser(
						TENANT_ID,
						"owner@example.com",
						"password123",
						Role.OWNER));

		verifyNoInteractions(tenantRepository, passwordEncoder);
		verify(userRepository, never()).saveAndFlush(any(User.class));
	}

	@Test
	void createUserRejectsDuplicateEmailBeforeLoadingTenant() {
		when(userRepository.existsByEmailIgnoreCase("staff@example.com")).thenReturn(true);

		assertThrows(
				DuplicateResourceException.class,
				() -> userManagementService.createUser(
						TENANT_ID,
						"STAFF@EXAMPLE.COM",
						"password123",
						Role.STAFF));

		verifyNoInteractions(tenantRepository, passwordEncoder);
	}

	@Test
	void updateEnabledChangesAStaffAccountWithinTheTenant() {
		User user = new User(
				tenant(),
				"staff@example.com",
				"encoded-password",
				Role.STAFF);
		when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID))
				.thenReturn(Optional.of(user));

		User updated = userManagementService.updateEnabled(TENANT_ID, USER_ID, false);

		assertSame(user, updated);
		assertFalse(updated.isEnabled());
	}

	@Test
	void updateEnabledRejectsCrossTenantUserAccess() {
		when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID))
				.thenReturn(Optional.empty());

		assertThrows(
				ResourceNotFoundException.class,
				() -> userManagementService.updateEnabled(TENANT_ID, USER_ID, false));
	}

	@Test
	void updateEnabledRejectsOwnerAccounts() {
		User owner = new User(
				tenant(),
				"owner@example.com",
				"encoded-password",
				Role.OWNER);
		when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID))
				.thenReturn(Optional.of(owner));

		assertThrows(
				InvalidOperationException.class,
				() -> userManagementService.updateEnabled(TENANT_ID, USER_ID, false));
	}

	private Tenant tenant() {
		return new Tenant("Glow Studio", "hello@example.com", null);
	}
}
