package com.bookflow.backend.customer;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

	Optional<Customer> findByIdAndTenantId(Long id, Long tenantId);

	Page<Customer> findAllByTenantId(Long tenantId, Pageable pageable);
}
