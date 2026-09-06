package com.bookflow.backend.user.dto;

import java.time.Instant;

import com.bookflow.backend.user.Role;
import com.bookflow.backend.user.User;

public record UserResponse(
		Long id,
		String email,
		Role role,
		boolean enabled,
		Instant createdAt) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getRole(),
				user.isEnabled(),
				user.getCreatedAt());
	}
}
