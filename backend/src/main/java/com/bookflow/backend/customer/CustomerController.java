package com.bookflow.backend.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookflow.backend.appointment.dto.AppointmentResponse;
import com.bookflow.backend.customer.dto.CustomerRequest;
import com.bookflow.backend.customer.dto.CustomerResponse;
import com.bookflow.backend.security.CurrentUserProvider;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

	private final CustomerService customerService;
	private final CurrentUserProvider currentUserProvider;

	@GetMapping
	public Page<CustomerResponse> getAllCustomers(
			@RequestParam(required = false) String search,
			@PageableDefault(
				size = 20,
				sort = {"lastName", "firstName", "id"}) Pageable pageable) {
		return customerService
				.getAllCustomers(currentUserProvider.getTenantId(), search, pageable)
				.map(CustomerResponse::from);
	}

	@GetMapping("/{customerId}/appointments")
	public Page<AppointmentResponse> getAppointmentHistory(
			@PathVariable Long customerId,
			@PageableDefault(
				size = 20,
				sort = {"startTime", "id"},
				direction = Sort.Direction.DESC) Pageable pageable) {
		return customerService
				.getAppointmentHistory(
						currentUserProvider.getTenantId(),
						customerId,
						pageable)
				.map(AppointmentResponse::from);
	}

	@GetMapping("/{customerId}")
	public CustomerResponse getCustomer(@PathVariable Long customerId) {
		return CustomerResponse.from(
				customerService.getCustomer(currentUserProvider.getTenantId(), customerId));
	}

	@PostMapping
	public ResponseEntity<CustomerResponse> createCustomer(
			@Valid @RequestBody CustomerRequest request) {
		Customer customer = customerService.createCustomer(
				currentUserProvider.getTenantId(),
				request.firstName(),
				request.lastName(),
				request.email(),
				request.phone(),
				request.notes());
		return ResponseEntity.status(HttpStatus.CREATED).body(CustomerResponse.from(customer));
	}

	@PutMapping("/{customerId}")
	public CustomerResponse updateCustomer(
			@PathVariable Long customerId,
			@Valid @RequestBody CustomerRequest request) {
		Customer customer = customerService.updateCustomer(
				currentUserProvider.getTenantId(),
				customerId,
				request.firstName(),
				request.lastName(),
				request.email(),
				request.phone(),
				request.notes());
		return CustomerResponse.from(customer);
	}
}
