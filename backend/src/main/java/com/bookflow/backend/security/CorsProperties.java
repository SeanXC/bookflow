package com.bookflow.backend.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Validated
@ConfigurationProperties(prefix = "bookflow.security.cors")
public class CorsProperties {

	@NotEmpty
	private List<@NotBlank String> allowedOrigins =
			new ArrayList<>(List.of("http://localhost:5173"));

	public List<String> getAllowedOrigins() {
		return List.copyOf(allowedOrigins);
	}

	public void setAllowedOrigins(List<String> allowedOrigins) {
		this.allowedOrigins = allowedOrigins == null
				? new ArrayList<>()
				: new ArrayList<>(allowedOrigins);
	}

	@AssertTrue(message = "CORS allowed origins must be explicit and cannot contain '*'")
	public boolean isWildcardDisabled() {
		return allowedOrigins.stream().noneMatch("*"::equals);
	}
}
