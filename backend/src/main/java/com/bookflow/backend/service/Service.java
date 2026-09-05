package com.bookflow.backend.service;

import java.math.BigDecimal;

import com.bookflow.backend.tenant.Tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "services")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Service {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tenant_id", nullable = false)
	private Tenant tenant;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Column(name = "duration_minutes", nullable = false)
	private int durationMinutes;

	@Column(nullable = false)
	private boolean active = true;

	public Service(
			Tenant tenant,
			String name,
			String description,
			BigDecimal price,
			int durationMinutes) {
		this.tenant = tenant;
		this.name = name;
		this.description = description;
		this.price = price;
		this.durationMinutes = durationMinutes;
	}

	public void updateDetails(
			String name,
			String description,
			BigDecimal price,
			int durationMinutes) {
		this.name = name;
		this.description = description;
		this.price = price;
		this.durationMinutes = durationMinutes;
	}

	public void deactivate() {
		this.active = false;
	}
}
