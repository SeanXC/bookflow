package com.bookflow.backend.ai;

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
class AssistantRateLimitFilterTest {

	@Mock
	private AssistantRateLimiter rateLimiter;

	private AssistantRateLimitFilter filter;

	@BeforeEach
	void setUp() {
		filter = new AssistantRateLimitFilter(rateLimiter, JsonMapper.builder().build());
	}

	@Test
	void rejectsExcessPublicAssistantChatAttempts() throws Exception {
		when(rateLimiter.tryConsume("203.0.113.10", "glow-studio")).thenReturn(false);
		when(rateLimiter.window()).thenReturn(Duration.ofMinutes(10));
		MockHttpServletRequest request = new MockHttpServletRequest(
				"POST",
				"/api/public/businesses/glow-studio/assistant");
		request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertEquals(429, response.getStatus());
		assertEquals("600", response.getHeader("Retry-After"));
		assertTrue(response.getContentAsString().contains("RATE_LIMITED"));
	}

	@Test
	void rateLimitsPublicConfirmAgainstTheSameSlug() throws Exception {
		when(rateLimiter.tryConsume("198.51.100.8", "glow-studio")).thenReturn(true);
		MockHttpServletRequest request = new MockHttpServletRequest(
				"POST",
				"/api/public/businesses/glow-studio/assistant/confirm");
		request.setRemoteAddr("198.51.100.8");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		verify(rateLimiter).tryConsume("198.51.100.8", "glow-studio");
		assertEquals(200, response.getStatus());
	}

	@Test
	void rateLimitsAuthenticatedAssistantRequests() throws Exception {
		when(rateLimiter.tryConsume("198.51.100.8", "authenticated")).thenReturn(true);
		MockHttpServletRequest request = new MockHttpServletRequest(
				"POST",
				"/api/assistant/confirm");
		request.setRemoteAddr("198.51.100.8");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		verify(rateLimiter).tryConsume("198.51.100.8", "authenticated");
		assertEquals(200, response.getStatus());
	}

	@Test
	void ignoresUnrelatedRequests() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(
				"GET",
				"/api/public/businesses/glow-studio/assistant");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		verify(rateLimiter, never()).tryConsume(any(), any());
		assertEquals(200, response.getStatus());
	}
}
