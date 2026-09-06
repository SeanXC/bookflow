package com.bookflow.backend.user;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmailIgnoreCase(String email);

	Optional<User> findByIdAndTenantId(Long id, Long tenantId);

	Page<User> findAllByTenantId(Long tenantId, Pageable pageable);

	boolean existsByEmailIgnoreCase(String email);
}
