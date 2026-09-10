package com.bookflow.backend.publicbooking;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublicBookingRateLimiterTest {

	private AtomicReference<Instant> now;
	private PublicBookingRateLimiter rateLimiter;

	@BeforeEach
	void setUp() {
		now = new AtomicReference<>(Instant.parse("2026-09-10T12:00:00Z"));
		rateLimiter = new PublicBookingRateLimiter(
				new PublicBookingRateLimitProperties(2, Duration.ofMinutes(10)),
				clock(now));
	}

	@Test
	void allowsRequestsUpToTheConfiguredLimit() {
		assertTrue(rateLimiter.tryConsume("1.1.1.1", "glow-studio"));
		assertTrue(rateLimiter.tryConsume("1.1.1.1", "glow-studio"));
		assertFalse(rateLimiter.tryConsume("1.1.1.1", "glow-studio"));
	}

	@Test
	void isolatesLimitsByIpAndSlug() {
		assertTrue(rateLimiter.tryConsume("1.1.1.1", "glow-studio"));
		assertTrue(rateLimiter.tryConsume("1.1.1.1", "glow-studio"));
		assertTrue(rateLimiter.tryConsume("2.2.2.2", "glow-studio"));
		assertTrue(rateLimiter.tryConsume("1.1.1.1", "other-salon"));
		assertFalse(rateLimiter.tryConsume("1.1.1.1", "glow-studio"));
	}

	@Test
	void expiresHitsAfterTheConfiguredWindow() {
		assertTrue(rateLimiter.tryConsume("1.1.1.1", "glow-studio"));
		assertTrue(rateLimiter.tryConsume("1.1.1.1", "glow-studio"));
		assertFalse(rateLimiter.tryConsume("1.1.1.1", "glow-studio"));

		now.set(Instant.parse("2026-09-10T12:10:01Z"));

		assertTrue(rateLimiter.tryConsume("1.1.1.1", "glow-studio"));
	}

	private Clock clock(AtomicReference<Instant> instant) {
		return new Clock() {
			@Override
			public ZoneOffset getZone() {
				return ZoneOffset.UTC;
			}

			@Override
			public Clock withZone(java.time.ZoneId zone) {
				return this;
			}

			@Override
			public Instant instant() {
				return instant.get();
			}
		};
	}
}
