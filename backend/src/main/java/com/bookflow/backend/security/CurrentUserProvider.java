package com.bookflow.backend.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.bookflow.backend.user.Role;

@Component
public class CurrentUserProvider {

	public AuthenticatedUser getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null
				|| !authentication.isAuthenticated()
				|| !(authentication.getPrincipal() instanceof AuthenticatedUser currentUser)) {
			throw new AuthenticationCredentialsNotFoundException(
					"An authenticated user is required");
		}

		return currentUser;
	}

	public Long getUserId() {
		return getCurrentUser().userId();
	}

	public Long getTenantId() {
		return getCurrentUser().tenantId();
	}

	public Role getRole() {
		return getCurrentUser().role();
	}
}
