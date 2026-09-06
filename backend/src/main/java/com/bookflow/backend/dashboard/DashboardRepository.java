package com.bookflow.backend.dashboard;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.bookflow.backend.appointment.Appointment;

public interface DashboardRepository extends Repository<Appointment, Long> {

	@Query(value = """
			WITH periods AS (
				SELECT CAST(:weekStart AS date) + series.day_offset AS period
				FROM generate_series(0, 6) AS series(day_offset)
			)
			SELECT periods.period AS period, COUNT(appointments.id) AS bookings
			FROM periods
			LEFT JOIN appointments
			  ON appointments.tenant_id = :tenantId
			 AND appointments.start_time
				 >= (CAST(periods.period AS timestamp) AT TIME ZONE :zoneId)
			 AND appointments.start_time
				 < (CAST(periods.period + 1 AS timestamp) AT TIME ZONE :zoneId)
			GROUP BY periods.period
			ORDER BY periods.period
			""", nativeQuery = true)
	List<DailyBookingView> findDailyBookingsForWeek(
			@Param("tenantId") Long tenantId,
			@Param("weekStart") LocalDate weekStart,
			@Param("zoneId") String zoneId);

	@Query(value = """
			WITH periods AS (
				SELECT CAST((
					CAST(:firstMonthStart AS date)
					+ series.month_offset * INTERVAL '1 month'
				) AS date) AS period
				FROM generate_series(0, 11) AS series(month_offset)
			)
			SELECT
				periods.period AS period,
				COALESCE(SUM(services.price), 0) AS revenue
			FROM periods
			LEFT JOIN appointments
			  ON appointments.tenant_id = :tenantId
			 AND appointments.status = 'COMPLETED'
			 AND appointments.start_time
				 >= (CAST(periods.period AS timestamp) AT TIME ZONE :zoneId)
			 AND appointments.start_time
				 < (
					CAST(periods.period + INTERVAL '1 month' AS timestamp)
					AT TIME ZONE :zoneId
				 )
			LEFT JOIN services
			  ON services.id = appointments.service_id
			 AND services.tenant_id = :tenantId
			GROUP BY periods.period
			ORDER BY periods.period
			""", nativeQuery = true)
	List<MonthlyRevenueView> findMonthlyRevenue(
			@Param("tenantId") Long tenantId,
			@Param("firstMonthStart") LocalDate firstMonthStart,
			@Param("zoneId") String zoneId);
}
