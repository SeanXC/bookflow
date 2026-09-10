package com.bookflow.backend.config;

import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

@Hidden
@SecurityRequirements
@RestController
@RequestMapping("/api/public/health")
public class HealthController {

	private final JdbcTemplate jdbcTemplate;

	public HealthController(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping
	public Map<String, String> health() {
		Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
		if (result == null || result != 1) {
			throw new IllegalStateException("Database health check failed");
		}
		return Map.of("status", "UP");
	}
}
