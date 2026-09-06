package com.bookflow.backend.staff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

import com.bookflow.backend.common.exception.DuplicateResourceException;
import com.bookflow.backend.common.exception.InvalidOperationException;
import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;
import com.bookflow.backend.user.Role;
import com.bookflow.backend.user.User;
import com.bookflow.backend.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

	private static final Long TENANT_ID = 10L;
	private static final Long STAFF_ID = 20L;
	private static final Long USER_ID = 30L;

	@Mock
	private StaffRepository staffRepository;

	@Mock
	private TenantRepository tenantRepository;

	@Mock
	private UserRepository userRepository;

	private StaffService staffService;

	@BeforeEach
	void setUp() {
		staffService = new StaffService(
				staffRepository,
				tenantRepository,
				userRepository);
	}

	@Test
	void createStaffLinksUserFromTheSameTenant() {
		Tenant tenant = tenant();
		User user = staffUser(tenant);
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID))
				.thenReturn(Optional.of(user));
		when(staffRepository.saveAndFlush(any(Staff.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Staff staff = staffService.createStaff(
				TENANT_ID,
				USER_ID,
				"Anna",
				"Smith",
				"0871234567");

		assertSame(tenant, staff.getTenant());
		assertSame(user, staff.getUser());
		assertEquals("Anna", staff.getFirstName());
		assertEquals("Smith", staff.getLastName());
		assertEquals("0871234567", staff.getPhone());
		assertTrue(staff.isActive());
	}

	@Test
	void createStaffAllowsARecordWithoutLoginAccount() {
		Tenant tenant = tenant();
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		when(staffRepository.saveAndFlush(any(Staff.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Staff staff = staffService.createStaff(
				TENANT_ID,
				null,
				"Anna",
				"Smith",
				null);

		assertNull(staff.getUser());
		verify(userRepository, never()).findByIdAndTenantId(any(), any());
	}

	@Test
	void createStaffRejectsUserOutsideTheTenant() {
		Tenant tenant = tenant();
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID))
				.thenReturn(Optional.empty());

		assertThrows(
				ResourceNotFoundException.class,
				() -> staffService.createStaff(
						TENANT_ID,
						USER_ID,
						"Anna",
						"Smith",
						null));

		verify(staffRepository, never()).saveAndFlush(any(Staff.class));
	}

	@Test
	void createStaffRejectsAReceptionistAccountLink() {
		Tenant tenant = tenant();
		User receptionist = new User(
				tenant,
				"reception@example.com",
				"encoded-password",
				Role.RECEPTIONIST);
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID))
				.thenReturn(Optional.of(receptionist));

		assertThrows(
				InvalidOperationException.class,
				() -> staffService.createStaff(
						TENANT_ID,
						USER_ID,
						"Anna",
						"Smith",
						null));

		verify(staffRepository, never()).saveAndFlush(any(Staff.class));
	}

	@Test
	void createStaffRejectsAnAccountAlreadyLinkedToAnotherStaffMember() {
		Tenant tenant = tenant();
		User user = staffUser(tenant);
		Staff linkedStaff = org.mockito.Mockito.mock(Staff.class);
		when(linkedStaff.getId()).thenReturn(99L);
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID))
				.thenReturn(Optional.of(user));
		when(staffRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
				.thenReturn(Optional.of(linkedStaff));

		assertThrows(
				DuplicateResourceException.class,
				() -> staffService.createStaff(
						TENANT_ID,
						USER_ID,
						"Anna",
						"Smith",
						null));

		verify(staffRepository, never()).saveAndFlush(any(Staff.class));
	}

	@Test
	void updateStaffChangesDetailsAndLinkedUser() {
		Tenant tenant = tenant();
		Staff staff = new Staff(tenant, null, "Anna", "Smith", "old-phone");
		User user = staffUser(tenant);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));
		when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID))
				.thenReturn(Optional.of(user));

		Staff updated = staffService.updateStaff(
				TENANT_ID,
				STAFF_ID,
				USER_ID,
				"Sophie",
				"Chen",
				"new-phone");

		assertSame(staff, updated);
		assertSame(user, updated.getUser());
		assertEquals("Sophie", updated.getFirstName());
		assertEquals("Chen", updated.getLastName());
		assertEquals("new-phone", updated.getPhone());
	}

	@Test
	void deactivateStaffUsesTenantScopedLookup() {
		Staff staff = new Staff(tenant(), null, "Anna", "Smith", null);
		when(staffRepository.findByIdAndTenantId(STAFF_ID, TENANT_ID))
				.thenReturn(Optional.of(staff));

		staffService.deactivateStaff(TENANT_ID, STAFF_ID);

		assertFalse(staff.isActive());
		verify(staffRepository).findByIdAndTenantId(STAFF_ID, TENANT_ID);
	}

	@Test
	void searchStaffCombinesEscapedNameAndActiveFilter() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<Staff> expected = Page.empty(pageable);
		when(staffRepository.searchByTenantId(
				TENANT_ID,
				"Anna!%",
				true,
				pageable))
				.thenReturn(expected);

		Page<Staff> result = staffService.getAllStaff(
				TENANT_ID,
				"  Anna%  ",
				true,
				pageable);

		assertSame(expected, result);
	}

	private Tenant tenant() {
		return new Tenant("Glow Studio", "hello@example.com", null);
	}

	private User staffUser(Tenant tenant) {
		return new User(
				tenant,
				"anna@example.com",
				"encoded-password",
				Role.STAFF);
	}
}
