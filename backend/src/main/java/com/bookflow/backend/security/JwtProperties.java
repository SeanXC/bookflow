package com.bookflow.backend.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Validated
@ConfigurationProperties(prefix = "bookflow.security.jwt")
public record JwtProperties(
		@NotBlank @Size(min = 32) String secret,
		@NotBlank String issuer,
		@NotNull Duration accessTokenTtl) {
}
