package com.bookflow.backend.dashboard.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

import io.swagger.v3.oas.annotations.media.Schema;

public record MonthlyRevenueResponse(
		@Schema(example = "2026-09")
		YearMonth month,
		@Schema(example = "8240.00")
		BigDecimal revenue) {
}
