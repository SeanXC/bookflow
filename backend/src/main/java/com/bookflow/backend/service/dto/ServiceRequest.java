package com.bookflow.backend.service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ServiceRequest(
		@NotBlank @Size(max = 150) String name,
		String description,
		@NotNull @DecimalMin("0.00") BigDecimal price,
		@Positive int durationMinutes) {
}
