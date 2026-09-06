package com.bookflow.backend.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

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

	private DashboardService dashboardService;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(
				Instant.parse("2026-09-06T12:00:00Z"),
				ZoneId.of("Europe/Dublin"));
		dashboardService = new DashboardService(
				appointmentRepository,
				customerRepository,
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
}
