package com.bookflow.backend.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookflow.backend.appointment.AppointmentRepository;
import com.bookflow.backend.appointment.AppointmentStatus;
import com.bookflow.backend.customer.CustomerRepository;
import com.bookflow.backend.dashboard.dto.DashboardSummaryResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

	private final AppointmentRepository appointmentRepository;
	private final CustomerRepository customerRepository;
	private final Clock businessClock;

	@PreAuthorize("hasRole('OWNER')")
	public DashboardSummaryResponse getSummary(Long tenantId) {
		ZoneId zone = businessClock.getZone();
		LocalDate today = LocalDate.now(businessClock);
		YearMonth currentMonth = YearMonth.from(today);

		Instant dayStart = today.atStartOfDay(zone).toInstant();
		Instant nextDayStart = today.plusDays(1).atStartOfDay(zone).toInstant();
		Instant monthStart = currentMonth.atDay(1).atStartOfDay(zone).toInstant();
		Instant nextMonthStart = currentMonth.plusMonths(1)
				.atDay(1)
				.atStartOfDay(zone)
				.toInstant();

		long todayAppointments =
				appointmentRepository
						.countByTenantIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(
								tenantId,
								dayStart,
								nextDayStart);
		BigDecimal monthlyRevenue =
				appointmentRepository.sumServiceRevenueByTenantIdAndStatusAndPeriod(
						tenantId,
						AppointmentStatus.COMPLETED,
						monthStart,
						nextMonthStart);
		long monthlyAppointments =
				appointmentRepository
						.countByTenantIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(
								tenantId,
								monthStart,
								nextMonthStart);
		long monthlyCancellations =
				appointmentRepository
						.countByTenantIdAndStatusAndStartTimeGreaterThanEqualAndStartTimeLessThan(
								tenantId,
								AppointmentStatus.CANCELLED,
								monthStart,
								nextMonthStart);

		return new DashboardSummaryResponse(
				todayAppointments,
				monthlyRevenue,
				customerRepository.countByTenantId(tenantId),
				calculateCancellationRate(monthlyCancellations, monthlyAppointments),
				zone.getId());
	}

	private BigDecimal calculateCancellationRate(long cancellations, long appointments) {
		if (appointments == 0) {
			return BigDecimal.ZERO.setScale(2);
		}
		return BigDecimal.valueOf(cancellations)
				.multiply(BigDecimal.valueOf(100))
				.divide(BigDecimal.valueOf(appointments), 2, RoundingMode.HALF_UP);
	}
}
