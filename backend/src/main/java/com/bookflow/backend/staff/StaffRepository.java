package com.bookflow.backend.staff;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface StaffRepository extends JpaRepository<Staff, Long> {

	Optional<Staff> findByIdAndTenantId(Long id, Long tenantId);

	Optional<Staff> findByUserIdAndTenantId(Long userId, Long tenantId);

	Page<Staff> findAllByTenantId(Long tenantId, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT staff
			FROM Staff staff
			WHERE staff.id = :staffId
			  AND staff.tenant.id = :tenantId
			""")
	Optional<Staff> findForUpdateByIdAndTenantId(
			@Param("staffId") Long staffId,
			@Param("tenantId") Long tenantId);
}
