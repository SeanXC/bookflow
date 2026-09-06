package com.bookflow.backend.user.dto;

import jakarta.validation.constraints.NotNull;

public record UserEnabledRequest(
		@NotNull Boolean enabled) {
}
