package com.bookflow.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Size(max = 150) String businessName,
		@NotBlank @Email @Size(max = 254) String businessEmail,
		@Size(max = 30) String businessPhone,
		@NotBlank @Email @Size(max = 254) String ownerEmail,
		@NotBlank @Size(min = 8, max = 72) String password) {
}
