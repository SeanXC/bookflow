package com.bookflow.backend.appointment;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.bookflow.backend.customer.Customer;
import com.bookflow.backend.service.Service;
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
@Table(name = "appointments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Appointment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tenant_id", nullable = false)
	private Tenant tenant;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "staff_id", nullable = false)
	private Staff staff;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "service_id", nullable = false)
	private Service service;

	@NotNull
	@Column(name = "start_time", nullable = false)
	private Instant startTime;

	@NotNull
	@Column(name = "end_time", nullable = false)
	private Instant endTime;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AppointmentStatus status = AppointmentStatus.CONFIRMED;

	@Column(columnDefinition = "TEXT")
	private String notes;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public Appointment(
			Tenant tenant,
			Customer customer,
			Staff staff,
			Service service,
			Instant startTime,
			Instant endTime,
			String notes) {
		this.tenant = tenant;
		this.customer = customer;
		this.staff = staff;
		this.service = service;
		this.startTime = startTime;
		this.endTime = endTime;
		this.notes = notes;
	}

	public void updateDetails(
			Customer customer,
			Staff staff,
			Service service,
			Instant startTime,
			Instant endTime,
			String notes) {
		this.customer = customer;
		this.staff = staff;
		this.service = service;
		this.startTime = startTime;
		this.endTime = endTime;
		this.notes = notes;
	}

	public void updateStatus(AppointmentStatus status) {
		this.status = status;
	}

	@AssertTrue(message = "end time must be after start time")
	public boolean isTimeRangeValid() {
		return startTime == null || endTime == null || endTime.isAfter(startTime);
	}
}
