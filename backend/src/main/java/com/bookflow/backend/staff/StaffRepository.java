package com.bookflow.backend.staff;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<Staff, Long> {

	Optional<Staff> findByIdAndTenantId(Long id, Long tenantId);

	Optional<Staff> findByUserIdAndTenantId(Long userId, Long tenantId);

	Page<Staff> findAllByTenantId(Long tenantId, Pageable pageable);
}
