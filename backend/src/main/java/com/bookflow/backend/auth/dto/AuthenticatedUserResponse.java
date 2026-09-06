package com.bookflow.backend.auth.dto;

import com.bookflow.backend.security.AuthenticatedUser;
import com.bookflow.backend.user.Role;

public record AuthenticatedUserResponse(
		String email,
		Role role) {

	public static AuthenticatedUserResponse from(AuthenticatedUser user) {
		return new AuthenticatedUserResponse(user.email(), user.role());
	}
}
