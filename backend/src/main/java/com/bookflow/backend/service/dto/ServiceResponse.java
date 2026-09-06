package com.bookflow.backend.service.dto;

import java.math.BigDecimal;

import com.bookflow.backend.service.Service;

public record ServiceResponse(
		Long id,
		String name,
		String description,
		BigDecimal price,
		int durationMinutes,
		boolean active) {

	public static ServiceResponse from(Service service) {
		return new ServiceResponse(
				service.getId(),
				service.getName(),
				service.getDescription(),
				service.getPrice(),
				service.getDurationMinutes(),
				service.isActive());
	}
}
