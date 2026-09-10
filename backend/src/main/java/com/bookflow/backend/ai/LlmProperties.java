package com.bookflow.backend.ai;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "bookflow.ai.llm")
public record LlmProperties(
		@NotBlank String baseUrl,
		String apiKey,
		@NotBlank String model,
		@NotNull Duration timeout) {

	public boolean isConfigured() {
		return apiKey != null && !apiKey.isBlank();
	}
}
