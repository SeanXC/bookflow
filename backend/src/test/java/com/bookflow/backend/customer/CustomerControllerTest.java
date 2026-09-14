package com.bookflow.backend.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import com.bookflow.backend.common.dto.PageResponse;
import com.bookflow.backend.customer.dto.CustomerRequest;
import com.bookflow.backend.customer.dto.CustomerResponse;
import com.bookflow.backend.security.CurrentUserProvider;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

	private static final Long TENANT_ID = 10L;
	private static final Long CUSTOMER_ID = 20L;

	@Mock
	private CustomerService customerService;

	@Mock
	private CurrentUserProvider currentUserProvider;

	@Mock
	private Customer customer;

	private CustomerController controller;

	@BeforeEach
	void setUp() {
		controller = new CustomerController(customerService, currentUserProvider);
	}

	@Test
	void listsTenantCustomersInTheStablePageEnvelope() {
		PageRequest pageable = PageRequest.of(1, 5);
		when(currentUserProvider.getTenantId()).thenReturn(TENANT_ID);
		when(customerService.getAllCustomers(TENANT_ID, "Ava", pageable))
				.thenReturn(new PageImpl<>(List.of(customer), pageable, 6));
		stubCustomer();

		PageResponse<CustomerResponse> response =
				controller.getAllCustomers("Ava", pageable);

		assertEquals(1, response.page());
		assertEquals(5, response.size());
		assertEquals(6, response.totalElements());
		assertEquals(CUSTOMER_ID, response.content().getFirst().id());
	}

	@Test
	void getsASelectedCustomer() {
		when(currentUserProvider.getTenantId()).thenReturn(TENANT_ID);
		when(customerService.getCustomer(TENANT_ID, CUSTOMER_ID)).thenReturn(customer);
		stubCustomer();

		CustomerResponse response = controller.getCustomer(CUSTOMER_ID);

		assertEquals(CUSTOMER_ID, response.id());
		assertEquals("Ava", response.firstName());
		assertEquals("Murphy", response.lastName());
	}

	@Test
	void createsAndUpdatesCustomersFromRequests() {
		CustomerRequest request = new CustomerRequest(
				"Ava",
				"Murphy",
				"ava@example.com",
				"0871234567",
				"Prefers afternoons");
		when(currentUserProvider.getTenantId()).thenReturn(TENANT_ID);
		when(customerService.createCustomer(
				TENANT_ID,
				request.firstName(),
				request.lastName(),
				request.email(),
				request.phone(),
				request.notes()))
				.thenReturn(customer);
		when(customerService.updateCustomer(
				TENANT_ID,
				CUSTOMER_ID,
				request.firstName(),
				request.lastName(),
				request.email(),
				request.phone(),
				request.notes()))
				.thenReturn(customer);
		stubCustomer();

		var created = controller.createCustomer(request);
		CustomerResponse updated = controller.updateCustomer(CUSTOMER_ID, request);

		assertEquals(HttpStatus.CREATED, created.getStatusCode());
		assertEquals(CUSTOMER_ID, created.getBody().id());
		assertEquals(CUSTOMER_ID, updated.id());
	}

	@Test
	void returnsAnEmptyAppointmentHistoryPage() {
		PageRequest pageable = PageRequest.of(0, 20);
		when(currentUserProvider.getTenantId()).thenReturn(TENANT_ID);
		when(customerService.getAppointmentHistory(TENANT_ID, CUSTOMER_ID, pageable))
				.thenReturn(new PageImpl<>(List.of(), pageable, 0));

		var response = controller.getAppointmentHistory(CUSTOMER_ID, pageable);

		assertTruePageIsEmpty(response);
		verify(customerService)
				.getAppointmentHistory(TENANT_ID, CUSTOMER_ID, pageable);
	}

	private void assertTruePageIsEmpty(PageResponse<?> response) {
		assertTrue(response.content().isEmpty());
		assertEquals(0, response.totalElements());
		assertEquals(0, response.totalPages());
	}

	private void stubCustomer() {
		Instant createdAt = Instant.parse("2026-09-12T10:00:00Z");
		when(customer.getId()).thenReturn(CUSTOMER_ID);
		when(customer.getFirstName()).thenReturn("Ava");
		when(customer.getLastName()).thenReturn("Murphy");
		when(customer.getEmail()).thenReturn("ava@example.com");
		when(customer.getPhone()).thenReturn("0871234567");
		when(customer.getNotes()).thenReturn("Prefers afternoons");
		when(customer.getCreatedAt()).thenReturn(createdAt);
	}
}
