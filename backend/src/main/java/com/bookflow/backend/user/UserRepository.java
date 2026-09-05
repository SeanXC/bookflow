package com.bookflow.backend.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmailIgnoreCase(String email);

	Optional<User> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByEmailIgnoreCase(String email);
}
