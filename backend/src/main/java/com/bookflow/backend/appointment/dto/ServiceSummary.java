package com.bookflow.backend.appointment.dto;

import java.math.BigDecimal;

import com.bookflow.backend.service.Service;

public record ServiceSummary(
		Long id,
		String name,
		BigDecimal price,
		int durationMinutes) {

	public static ServiceSummary from(Service service) {
		return new ServiceSummary(
				service.getId(),
				service.getName(),
				service.getPrice(),
				service.getDurationMinutes());
	}
}
