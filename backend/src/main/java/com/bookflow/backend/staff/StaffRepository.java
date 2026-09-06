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

	@Query("""
			SELECT staff
			FROM Staff staff
			WHERE staff.tenant.id = :tenantId
			  AND (:active IS NULL OR staff.active = :active)
			  AND (
				LOWER(staff.firstName)
					LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!'
				OR LOWER(staff.lastName)
					LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!'
				OR LOWER(CONCAT(CONCAT(staff.firstName, ' '), staff.lastName))
					LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!'
			  )
			""")
	Page<Staff> searchByTenantId(
			@Param("tenantId") Long tenantId,
			@Param("search") String search,
			@Param("active") Boolean active,
			Pageable pageable);

	Page<Staff> findAllByTenantIdAndActive(
			Long tenantId,
			boolean active,
			Pageable pageable);

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
