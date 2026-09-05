package com.bookflow.backend.staff;

import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "staff")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Staff {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tenant_id", nullable = false)
	private Tenant tenant;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", unique = true)
	private User user;

	@NotBlank
	@Size(max = 100)
	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@NotBlank
	@Size(max = 100)
	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Size(max = 30)
	@Column(length = 30)
	private String phone;

	@Column(nullable = false)
	private boolean active = true;

	public Staff(Tenant tenant, User user, String firstName, String lastName, String phone) {
		this.tenant = tenant;
		this.user = user;
		this.firstName = firstName;
		this.lastName = lastName;
		this.phone = phone;
	}

	public void updateDetails(User user, String firstName, String lastName, String phone) {
		this.user = user;
		this.firstName = firstName;
		this.lastName = lastName;
		this.phone = phone;
	}

	public void deactivate() {
		this.active = false;
	}
}
