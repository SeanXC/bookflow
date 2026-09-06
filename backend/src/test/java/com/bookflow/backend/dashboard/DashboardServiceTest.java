package com.bookflow.backend.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bookflow.backend.appointment.AppointmentRepository;
import com.bookflow.backend.appointment.AppointmentStatus;
import com.bookflow.backend.customer.CustomerRepository;
import com.bookflow.backend.dashboard.dto.DashboardSummaryResponse;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

	private static final Long TENANT_ID = 42L;
	private static final Instant DAY_START = Instant.parse("2026-09-05T23:00:00Z");
	private static final Instant NEXT_DAY_START = Instant.parse("2026-09-06T23:00:00Z");
	private static final Instant MONTH_START = Instant.parse("2026-08-31T23:00:00Z");
	private static final Instant NEXT_MONTH_START = Instant.parse("2026-09-30T23:00:00Z");

	@Mock
	private AppointmentRepository appointmentRepository;

	@Mock
	private CustomerRepository customerRepository;

	@Mock
	private DashboardRepository dashboardRepository;

	private DashboardService dashboardService;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(
				Instant.parse("2026-09-06T12:00:00Z"),
				ZoneId.of("Europe/Dublin"));
		dashboardService = new DashboardService(
				appointmentRepository,
				customerRepository,
				dashboardRepository,
				clock);
	}

	@Test
	void summaryUsesTenantScopeBusinessTimeZoneAndCurrentMonthMetrics() {
		when(appointmentRepository
				.countByTenantIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(
						TENANT_ID,
						DAY_START,
						NEXT_DAY_START))
				.thenReturn(18L);
		when(appointmentRepository.sumServiceRevenueByTenantIdAndStatusAndPeriod(
				TENANT_ID,
				AppointmentStatus.COMPLETED,
				MONTH_START,
				NEXT_MONTH_START))
				.thenReturn(new BigDecimal("8240.00"));
		when(appointmentRepository
				.countByTenantIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(
						TENANT_ID,
						MONTH_START,
						NEXT_MONTH_START))
				.thenReturn(24L);
		when(appointmentRepository
				.countByTenantIdAndStatusAndStartTimeGreaterThanEqualAndStartTimeLessThan(
						TENANT_ID,
						AppointmentStatus.CANCELLED,
						MONTH_START,
						NEXT_MONTH_START))
				.thenReturn(1L);
		when(customerRepository.countByTenantId(TENANT_ID)).thenReturn(126L);

		DashboardSummaryResponse summary = dashboardService.getSummary(TENANT_ID);

		assertEquals(18L, summary.todayAppointments());
		assertEquals(new BigDecimal("8240.00"), summary.monthlyRevenue());
		assertEquals(126L, summary.activeCustomers());
		assertEquals(new BigDecimal("4.17"), summary.cancellationRate());
		assertEquals("Europe/Dublin", summary.businessTimeZone());
	}

	@Test
	void cancellationRateIsZeroWhenTheMonthHasNoAppointments() {
		when(appointmentRepository.sumServiceRevenueByTenantIdAndStatusAndPeriod(
				TENANT_ID,
				AppointmentStatus.COMPLETED,
				MONTH_START,
				NEXT_MONTH_START))
				.thenReturn(BigDecimal.ZERO);

		DashboardSummaryResponse summary = dashboardService.getSummary(TENANT_ID);

		assertEquals(new BigDecimal("0.00"), summary.cancellationRate());
	}

	@Test
	void bookingsSeriesUsesTheCurrentIsoWeekAndPreservesOrder() {
		DailyBookingView monday = dailyBooking(
				LocalDate.parse("2026-08-31"),
				2L);
		DailyBookingView tuesday = dailyBooking(
				LocalDate.parse("2026-09-01"),
				0L);
		when(dashboardRepository.findDailyBookingsForWeek(
				TENANT_ID,
				LocalDate.parse("2026-08-31"),
				"Europe/Dublin"))
				.thenReturn(List.of(monday, tuesday));

		var series = dashboardService.getBookingsByWeek(TENANT_ID);

		assertEquals(2, series.size());
		assertEquals(LocalDate.parse("2026-08-31"), series.get(0).date());
		assertEquals(2L, series.get(0).bookings());
		assertEquals(LocalDate.parse("2026-09-01"), series.get(1).date());
		assertEquals(0L, series.get(1).bookings());
	}

	@Test
	void revenueSeriesStartsElevenMonthsBeforeTheCurrentMonth() {
		MonthlyRevenueView firstMonth = monthlyRevenue(
				LocalDate.parse("2025-10-01"),
				new BigDecimal("125.00"));
		when(dashboardRepository.findMonthlyRevenue(
				TENANT_ID,
				LocalDate.parse("2025-10-01"),
				"Europe/Dublin"))
				.thenReturn(List.of(firstMonth));

		var series = dashboardService.getRevenueByMonth(TENANT_ID);

		assertEquals(1, series.size());
		assertEquals("2025-10", series.getFirst().month().toString());
		assertEquals(new BigDecimal("125.00"), series.getFirst().revenue());
	}

	private DailyBookingView dailyBooking(LocalDate period, long bookings) {
		DailyBookingView view = mock(DailyBookingView.class);
		when(view.getPeriod()).thenReturn(period);
		when(view.getBookings()).thenReturn(bookings);
		return view;
	}

	private MonthlyRevenueView monthlyRevenue(LocalDate period, BigDecimal revenue) {
		MonthlyRevenueView view = mock(MonthlyRevenueView.class);
		when(view.getPeriod()).thenReturn(period);
		when(view.getRevenue()).thenReturn(revenue);
		return view;
	}
}
