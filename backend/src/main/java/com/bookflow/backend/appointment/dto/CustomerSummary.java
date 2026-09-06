package com.bookflow.backend.appointment.dto;

import com.bookflow.backend.customer.Customer;

public record CustomerSummary(
		Long id,
		String firstName,
		String lastName,
		String email,
		String phone) {

	public static CustomerSummary from(Customer customer) {
		return new CustomerSummary(
				customer.getId(),
				customer.getFirstName(),
				customer.getLastName(),
				customer.getEmail(),
				customer.getPhone());
	}
}
