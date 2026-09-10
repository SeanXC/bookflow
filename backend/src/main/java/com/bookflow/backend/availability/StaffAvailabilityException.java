package com.bookflow.backend.availability;

import java.time.LocalDate;
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
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "staff_availability_exceptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffAvailabilityException {

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
	@Column(name = "exception_date", nullable = false)
	private LocalDate exceptionDate;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AvailabilityExceptionType type;

	@Column(name = "start_time")
	private LocalTime startTime;

	@Column(name = "end_time")
	private LocalTime endTime;

	@Size(max = 255)
	@Column(length = 255)
	private String note;

	public StaffAvailabilityException(
			Tenant tenant,
			Staff staff,
			LocalDate exceptionDate,
			AvailabilityExceptionType type,
			LocalTime startTime,
			LocalTime endTime,
			String note) {
		this.tenant = tenant;
		this.staff = staff;
		this.exceptionDate = exceptionDate;
		this.type = type;
		this.startTime = startTime;
		this.endTime = endTime;
		this.note = note;
	}

	public void updateDetails(
			LocalDate exceptionDate,
			AvailabilityExceptionType type,
			LocalTime startTime,
			LocalTime endTime,
			String note) {
		this.exceptionDate = exceptionDate;
		this.type = type;
		this.startTime = startTime;
		this.endTime = endTime;
		this.note = note;
	}

	@AssertTrue(message = "exception time range must be complete and ordered")
	public boolean isTimeRangeValid() {
		if (startTime == null && endTime == null) {
			return type != AvailabilityExceptionType.CUSTOM_HOURS;
		}
		return startTime != null && endTime != null && endTime.isAfter(startTime);
	}
}
