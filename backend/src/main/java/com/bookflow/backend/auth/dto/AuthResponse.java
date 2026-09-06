package com.bookflow.backend.auth.dto;

public record AuthResponse(
		String accessToken,
		AuthenticatedUserResponse user) {
}
