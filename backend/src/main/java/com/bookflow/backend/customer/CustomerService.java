package com.bookflow.backend.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookflow.backend.common.exception.ResourceNotFoundException;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

	private final CustomerRepository customerRepository;
	private final TenantRepository tenantRepository;

	public Customer getCustomer(Long tenantId, Long customerId) {
		return customerRepository.findByIdAndTenantId(customerId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
	}

	public Page<Customer> getAllCustomers(Long tenantId, Pageable pageable) {
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
}
