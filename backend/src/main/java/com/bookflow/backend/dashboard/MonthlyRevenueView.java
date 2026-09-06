package com.bookflow.backend.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface MonthlyRevenueView {

	LocalDate getPeriod();

	BigDecimal getRevenue();
}
