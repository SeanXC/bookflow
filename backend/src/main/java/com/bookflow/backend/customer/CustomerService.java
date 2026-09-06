package com.bookflow.backend.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST')")
public class CustomerService {

	private final CustomerRepository customerRepository;
	private final TenantRepository tenantRepository;

	public Customer getCustomer(Long tenantId, Long customerId) {
		return customerRepository.findByIdAndTenantId(customerId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
	}

	public Page<Customer> getAllCustomers(
			Long tenantId,
			String search,
			Pageable pageable) {
		if (search != null && !search.isBlank()) {
			return customerRepository.searchByTenantId(
					tenantId,
					escapeLikePattern(search.trim()),
					pageable);
		}
		return customerRepository.findAllByTenantId(tenantId, pageable);
	}

	@Transactional
	public Customer createCustomer(
			Long tenantId,
			String firstName,
			String lastName,
			String email,
			String phone,
			String notes) {
		Tenant tenant = tenantRepository.findById(tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

		return customerRepository.save(
				new Customer(tenant, firstName, lastName, email, phone, notes));
	}

	@Transactional
	public Customer updateCustomer(
			Long tenantId,
			Long customerId,
			String firstName,
			String lastName,
			String email,
			String phone,
			String notes) {
		Customer customer = getCustomer(tenantId, customerId);
		customer.updateDetails(firstName, lastName, email, phone, notes);
		return customer;
	}

	private String escapeLikePattern(String value) {
		return value
				.replace("!", "!!")
				.replace("%", "!%")
				.replace("_", "!_");
	}
}
