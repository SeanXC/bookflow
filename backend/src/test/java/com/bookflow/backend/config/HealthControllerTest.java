package com.bookflow.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

	@Mock
	private JdbcTemplate jdbcTemplate;

	private HealthController controller;

	@BeforeEach
	void setUp() {
		controller = new HealthController(jdbcTemplate);
	}

	@Test
	void reportsUpWhenTheDatabaseResponds() {
		when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

		Map<String, String> response = controller.health();

		assertEquals(Map.of("status", "UP"), response);
	}

	@Test
	void rejectsNullAndUnexpectedDatabaseResponses() {
		when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
				.thenReturn(null, 2);

		assertThrows(IllegalStateException.class, controller::health);
		assertThrows(IllegalStateException.class, controller::health);
	}
}
