package com.bookflow.backend.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<Service, Long> {

	Optional<Service> findByIdAndTenantId(Long id, Long tenantId);

	Page<Service> findAllByTenantId(Long tenantId, Pageable pageable);
}
