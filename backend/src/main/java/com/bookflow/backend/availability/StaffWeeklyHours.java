package com.bookflow.backend.availability;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.bookflow.backend.staff.Staff;
import com.bookflow.backend.tenant.Tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "staff_weekly_hours")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffWeeklyHours {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tenant_id", nullable = false)
	private Tenant tenant;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "staff_id", nullable = false)
	private Staff staff;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "day_of_week", nullable = false, length = 9)
	private DayOfWeek dayOfWeek;

	@NotNull
	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	@NotNull
	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;

	public StaffWeeklyHours(
			Tenant tenant,
			Staff staff,
			DayOfWeek dayOfWeek,
			LocalTime startTime,
			LocalTime endTime) {
		this.tenant = tenant;
		this.staff = staff;
		this.dayOfWeek = dayOfWeek;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public void updateWindow(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
		this.dayOfWeek = dayOfWeek;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	@AssertTrue(message = "end time must be after start time")
	public boolean isTimeRangeValid() {
		return startTime == null || endTime == null || endTime.isAfter(startTime);
	}
}
