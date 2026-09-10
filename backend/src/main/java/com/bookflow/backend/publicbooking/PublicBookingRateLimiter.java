package com.bookflow.backend.publicbooking;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;

@Component
public class PublicBookingRateLimiter {

	private final PublicBookingRateLimitProperties properties;
	private final Clock clock;
	private final ConcurrentHashMap<String, List<Long>> windows = new ConcurrentHashMap<>();

	public PublicBookingRateLimiter(
			PublicBookingRateLimitProperties properties,
			Clock clock) {
		this.properties = properties;
		this.clock = clock;
	}

	public boolean tryConsume(String clientIp, String slug) {
		String key = clientIp + "\n" + slug;
		Instant now = Instant.now(clock);
		long cutoff = now.minus(properties.window()).toEpochMilli();
		long nowMillis = now.toEpochMilli();
		int maxRequests = properties.maxRequests();
		AtomicBoolean allowed = new AtomicBoolean(false);

		windows.compute(key, (ignored, timestamps) -> {
			List<Long> kept = new ArrayList<>();
			if (timestamps != null) {
				for (Long timestamp : timestamps) {
					if (timestamp > cutoff) {
						kept.add(timestamp);
					}
				}
			}
			if (kept.size() < maxRequests) {
				kept.add(nowMillis);
				allowed.set(true);
			}
			return kept.isEmpty() ? null : kept;
		});
		return allowed.get();
	}

	public Duration window() {
		return properties.window();
	}
}
