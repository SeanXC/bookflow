package com.bookflow.backend.customer.dto;

import java.time.Instant;

import com.bookflow.backend.customer.Customer;

public record CustomerResponse(
		Long id,
		String firstName,
		String lastName,
		String email,
		String phone,
		String notes,
		Instant createdAt) {

	public static CustomerResponse from(Customer customer) {
		return new CustomerResponse(
				customer.getId(),
				customer.getFirstName(),
				customer.getLastName(),
				customer.getEmail(),
				customer.getPhone(),
				customer.getNotes(),
				customer.getCreatedAt());
	}
}
