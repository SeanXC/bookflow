package com.bookflow.backend.dashboard.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

public record DashboardSummaryResponse(
		@Schema(example = "18")
		long todayAppointments,
		@Schema(example = "8240.00")
		BigDecimal monthlyRevenue,
		@Schema(example = "126")
		long activeCustomers,
		@Schema(description = "Current-month cancellation percentage", example = "4.20")
		BigDecimal cancellationRate,
		@Schema(example = "Europe/Dublin")
		String businessTimeZone) {
}
