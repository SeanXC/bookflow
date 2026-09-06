package com.bookflow.backend.user.dto;

import com.bookflow.backend.user.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(
		@NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Size(min = 8, max = 72) String password,
		@NotNull Role role) {
}
