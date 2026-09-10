package com.bookflow.backend.publicbooking.dto;

import java.math.BigDecimal;

import com.bookflow.backend.service.Service;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Publicly bookable service")
public record PublicServiceResponse(
		Long id,
		String name,
		String description,
		BigDecimal price,
		int durationMinutes) {

	public static PublicServiceResponse from(Service service) {
		return new PublicServiceResponse(
				service.getId(),
				service.getName(),
				service.getDescription(),
				service.getPrice(),
				service.getDurationMinutes());
	}
}
