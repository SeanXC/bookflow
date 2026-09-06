package com.bookflow.backend.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import com.bookflow.backend.appointment.Appointment;
import com.bookflow.backend.appointment.AppointmentRepository;
import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

	private static final Long TENANT_ID = 10L;
	private static final Long CUSTOMER_ID = 20L;

	@Mock
	private CustomerRepository customerRepository;

	@Mock
	private TenantRepository tenantRepository;

	@Mock
	private AppointmentRepository appointmentRepository;

	private CustomerService customerService;

	@BeforeEach
	void setUp() {
		customerService = new CustomerService(
				customerRepository,
				tenantRepository,
				appointmentRepository);
	}

	@Test
	void createCustomerUsesTheAuthenticatedTenant() {
		Tenant tenant = tenant();
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		when(customerRepository.save(any(Customer.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Customer customer = customerService.createCustomer(
				TENANT_ID,
				"Emma",
				"Smith",
				"emma@example.com",
				"0871234567",
				"Prefers morning appointments");

		assertSame(tenant, customer.getTenant());
		assertEquals("Emma", customer.getFirstName());
		assertEquals("Smith", customer.getLastName());
		assertEquals("emma@example.com", customer.getEmail());
		assertEquals("0871234567", customer.getPhone());
		assertEquals("Prefers morning appointments", customer.getNotes());
	}

	@Test
	void updateCustomerUsesTenantScopedLookupAndChangesDetails() {
		Customer customer = customer();
		when(customerRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
				.thenReturn(Optional.of(customer));

		Customer updated = customerService.updateCustomer(
				TENANT_ID,
				CUSTOMER_ID,
				"Sarah",
				"Jones",
				"sarah@example.com",
				"0857654321",
				"Updated notes");

		assertSame(customer, updated);
		assertEquals("Sarah", updated.getFirstName());
		assertEquals("Jones", updated.getLastName());
		assertEquals("sarah@example.com", updated.getEmail());
		assertEquals("0857654321", updated.getPhone());
		assertEquals("Updated notes", updated.getNotes());
		verify(customerRepository).findByIdAndTenantId(CUSTOMER_ID, TENANT_ID);
	}

	@Test
	void getCustomerRejectsCrossTenantAccessAsNotFound() {
		when(customerRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
				.thenReturn(Optional.empty());

		assertThrows(
				ResourceNotFoundException.class,
				() -> customerService.getCustomer(TENANT_ID, CUSTOMER_ID));
	}

	@Test
	void searchCustomersEscapesLikeWildcardsAndUsesTenantScope() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<Customer> expected = Page.empty(pageable);
		when(customerRepository.searchByTenantId(
				TENANT_ID,
				"!%!_!!",
				pageable))
				.thenReturn(expected);

		Page<Customer> result = customerService.getAllCustomers(
				TENANT_ID,
				"  %_!  ",
				pageable);

		assertSame(expected, result);
	}

	@Test
	void blankCustomerSearchUsesTheTenantListQuery() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<Customer> expected = Page.empty(pageable);
		when(customerRepository.findAllByTenantId(TENANT_ID, pageable))
				.thenReturn(expected);

		Page<Customer> result = customerService.getAllCustomers(
				TENANT_ID,
				"  ",
				pageable);

		assertSame(expected, result);
		verify(customerRepository, never()).searchByTenantId(any(), any(), any());
	}

	@Test
	void getAppointmentHistoryValidatesCustomerBeforeQueryingAppointments() {
		Customer customer = customer();
		Pageable pageable = PageRequest.of(0, 20);
		Page<Appointment> expected = Page.empty(pageable);
		when(customerRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
				.thenReturn(Optional.of(customer));
		when(appointmentRepository.findAllByTenantIdAndCustomerId(
				TENANT_ID,
				CUSTOMER_ID,
				pageable))
				.thenReturn(expected);

		Page<Appointment> result = customerService.getAppointmentHistory(
				TENANT_ID,
				CUSTOMER_ID,
				pageable);

		assertSame(expected, result);
		verify(appointmentRepository).findAllByTenantIdAndCustomerId(
				TENANT_ID,
				CUSTOMER_ID,
				pageable);
	}

	@Test
	void getAppointmentHistoryDoesNotQueryAppointmentsForAnotherTenantCustomer() {
		Pageable pageable = PageRequest.of(0, 20);
		when(customerRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
				.thenReturn(Optional.empty());

		assertThrows(
				ResourceNotFoundException.class,
				() -> customerService.getAppointmentHistory(
						TENANT_ID,
						CUSTOMER_ID,
						pageable));

		verify(appointmentRepository, never()).findAllByTenantIdAndCustomerId(
				any(),
				any(),
				any());
	}

	private Tenant tenant() {
		return new Tenant("Glow Studio", "hello@example.com", null);
	}

	private Customer customer() {
		return new Customer(
				tenant(),
				"Emma",
				"Smith",
				"emma@example.com",
				"0871234567",
				null);
	}
}
