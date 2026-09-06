package com.bookflow.backend.customer;

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
import com.bookflow.backend.common.dto.PageResponse;
import com.bookflow.backend.common.error.ApiErrorResponse;
import com.bookflow.backend.customer.dto.CustomerRequest;
import com.bookflow.backend.customer.dto.CustomerResponse;
import com.bookflow.backend.security.CurrentUserProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Tenant-scoped customer management")
@ApiResponses({
	@ApiResponse(responseCode = "400", description = "Invalid request",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "401", description = "Authentication required",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "403", description = "Insufficient role permission",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
	@ApiResponse(responseCode = "404", description = "Customer not found",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class CustomerController {

	private final CustomerService customerService;
	private final CurrentUserProvider currentUserProvider;

	@GetMapping
	@Operation(summary = "List and search customers")
	public PageResponse<CustomerResponse> getAllCustomers(
			@Parameter(description = "Case-insensitive name, email, or phone search")
			@RequestParam(required = false) String search,
			@PageableDefault(
				size = 20,
				sort = {"lastName", "firstName", "id"}) Pageable pageable) {
		return PageResponse.from(customerService
				.getAllCustomers(currentUserProvider.getTenantId(), search, pageable)
				.map(CustomerResponse::from));
	}

	@GetMapping("/{customerId}/appointments")
	@Operation(summary = "List a customer's appointment history")
	public PageResponse<AppointmentResponse> getAppointmentHistory(
			@PathVariable Long customerId,
			@PageableDefault(
				size = 20,
				sort = {"startTime", "id"},
				direction = Sort.Direction.DESC) Pageable pageable) {
		return PageResponse.from(customerService
				.getAppointmentHistory(
						currentUserProvider.getTenantId(),
						customerId,
						pageable)
				.map(AppointmentResponse::from));
	}

	@GetMapping("/{customerId}")
	@Operation(summary = "Get a customer")
	public CustomerResponse getCustomer(@PathVariable Long customerId) {
		return CustomerResponse.from(
				customerService.getCustomer(currentUserProvider.getTenantId(), customerId));
	}

	@PostMapping
	@Operation(summary = "Create a customer")
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
	@Operation(summary = "Update a customer")
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
