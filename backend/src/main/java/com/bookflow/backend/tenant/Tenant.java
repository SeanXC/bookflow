package com.bookflow.backend.tenant;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.bookflow.backend.publicbooking.BookingSlug;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tenants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 150)
	@Column(nullable = false, length = 150)
	private String name;

	@NotBlank
	@Email
	@Size(max = 254)
	@Column(nullable = false, length = 254)
	private String email;

	@Size(max = 30)
	@Column(length = 30)
	private String phone;

	@NotBlank
	@Size(min = BookingSlug.MIN_LENGTH, max = BookingSlug.MAX_LENGTH)
	@Pattern(regexp = BookingSlug.PATTERN)
	@Column(nullable = false, unique = true, length = BookingSlug.MAX_LENGTH)
	private String slug;

	@Size(max = 500)
	@Column(length = 500)
	private String description;

	@Column(name = "public_booking_enabled", nullable = false)
	private boolean publicBookingEnabled = true;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public Tenant(String name, String email, String phone) {
		this.name = name;
		this.email = email;
		this.phone = phone;
		this.slug = BookingSlug.from(name);
	}

	public void assignSlug(String slug) {
		this.slug = slug;
	}

	public void updatePublicProfile(
			String name,
			String phone,
			String slug,
			String description,
			boolean publicBookingEnabled) {
		this.name = name;
		this.phone = phone;
		this.slug = slug;
		this.description = description;
		this.publicBookingEnabled = publicBookingEnabled;
	}
}
