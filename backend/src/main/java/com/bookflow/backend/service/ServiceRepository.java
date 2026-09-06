package com.bookflow.backend.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceRepository extends JpaRepository<Service, Long> {

	Optional<Service> findByIdAndTenantId(Long id, Long tenantId);

	Page<Service> findAllByTenantId(Long tenantId, Pageable pageable);

	@Query("""
			SELECT service
			FROM Service service
			WHERE service.tenant.id = :tenantId
			  AND (:active IS NULL OR service.active = :active)
			  AND (
				LOWER(service.name)
					LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!'
				OR LOWER(service.description)
					LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!'
			  )
			""")
	Page<Service> searchByTenantId(
			@Param("tenantId") Long tenantId,
			@Param("search") String search,
			@Param("active") Boolean active,
			Pageable pageable);

	Page<Service> findAllByTenantIdAndActive(
			Long tenantId,
			boolean active,
			Pageable pageable);
}
