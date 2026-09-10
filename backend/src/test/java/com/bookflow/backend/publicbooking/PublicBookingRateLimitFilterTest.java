package com.bookflow.backend.publicbooking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class PublicBookingRateLimitFilterTest {

	@Mock
	private PublicBookingRateLimiter rateLimiter;

	private PublicBookingRateLimitFilter filter;

	@BeforeEach
	void setUp() {
		filter = new PublicBookingRateLimitFilter(rateLimiter, JsonMapper.builder().build());
	}

	@Test
	void rejectsExcessPublicCreateAttempts() throws Exception {
		when(rateLimiter.tryConsume("203.0.113.10", "glow-studio")).thenReturn(false);
		when(rateLimiter.window()).thenReturn(Duration.ofMinutes(10));
		MockHttpServletRequest request = new MockHttpServletRequest(
				"POST",
				"/api/public/businesses/glow-studio/appointments");
		request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertEquals(429, response.getStatus());
		assertEquals("600", response.getHeader("Retry-After"));
		assertTrue(response.getContentAsString().contains("RATE_LIMITED"));
	}

	@Test
	void ignoresNonCreateRequests() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(
				"GET",
				"/api/public/businesses/glow-studio/appointments");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		verify(rateLimiter, never()).tryConsume(any(), any());
		assertEquals(200, response.getStatus());
	}

	@Test
	void allowsCreateAttemptsUnderTheLimit() throws Exception {
		when(rateLimiter.tryConsume("198.51.100.8", "glow-studio")).thenReturn(true);
		MockHttpServletRequest request = new MockHttpServletRequest(
				"POST",
				"/api/public/businesses/glow-studio/appointments");
		request.setRemoteAddr("198.51.100.8");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		verify(rateLimiter).tryConsume("198.51.100.8", "glow-studio");
		assertEquals(200, response.getStatus());
	}
}
