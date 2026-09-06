package com.bookflow.backend.user;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tenant_id", nullable = false)
	private Tenant tenant;

	@NotBlank
	@Email
	@Size(max = 254)
	@Column(nullable = false, unique = true, length = 254)
	private String email;

	@NotBlank
	@Size(max = 255)
	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Column(nullable = false)
	private boolean enabled = true;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public User(Tenant tenant, String email, String passwordHash, Role role) {
		this.tenant = tenant;
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
	}

	public void updateEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
