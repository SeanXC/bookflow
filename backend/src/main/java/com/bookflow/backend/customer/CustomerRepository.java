package com.bookflow.backend.customer;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

	Optional<Customer> findByIdAndTenantId(Long id, Long tenantId);

	long countByTenantId(Long tenantId);

	Page<Customer> findAllByTenantId(Long tenantId, Pageable pageable);

	@Query("""
			SELECT customer
			FROM Customer customer
			WHERE customer.tenant.id = :tenantId
			  AND (
				LOWER(customer.firstName)
					LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!'
				OR LOWER(customer.lastName)
					LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!'
				OR LOWER(CONCAT(CONCAT(customer.firstName, ' '), customer.lastName))
					LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!'
				OR LOWER(customer.email)
					LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!'
				OR LOWER(customer.phone)
					LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!'
			  )
			""")
	Page<Customer> searchByTenantId(
			@Param("tenantId") Long tenantId,
			@Param("search") String search,
			Pageable pageable);
}
