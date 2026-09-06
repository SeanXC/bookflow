package com.bookflow.backend.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookflow.backend.appointment.AppointmentRepository;
import com.bookflow.backend.appointment.AppointmentStatus;
import com.bookflow.backend.customer.CustomerRepository;
import com.bookflow.backend.dashboard.dto.DailyBookingResponse;
import com.bookflow.backend.dashboard.dto.DashboardSummaryResponse;
import com.bookflow.backend.dashboard.dto.MonthlyRevenueResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

	private final AppointmentRepository appointmentRepository;
	private final CustomerRepository customerRepository;
	private final DashboardRepository dashboardRepository;
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

	@PreAuthorize("hasRole('OWNER')")
	public List<DailyBookingResponse> getBookingsByWeek(Long tenantId) {
		LocalDate weekStart = LocalDate.now(businessClock)
				.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

		return dashboardRepository.findDailyBookingsForWeek(
				tenantId,
				weekStart,
				businessClock.getZone().getId())
				.stream()
				.map(point -> new DailyBookingResponse(
						point.getPeriod(),
						point.getBookings()))
				.toList();
	}

	@PreAuthorize("hasRole('OWNER')")
	public List<MonthlyRevenueResponse> getRevenueByMonth(Long tenantId) {
		YearMonth firstMonth = YearMonth.now(businessClock).minusMonths(11);

		return dashboardRepository.findMonthlyRevenue(
				tenantId,
				firstMonth.atDay(1),
				businessClock.getZone().getId())
				.stream()
				.map(point -> new MonthlyRevenueResponse(
						YearMonth.from(point.getPeriod()),
						point.getRevenue()))
				.toList();
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
