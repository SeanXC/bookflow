package com.bookflow.backend.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bookflow.backend.ai.dto.AssistantProposalResponse;
import com.bookflow.backend.common.exception.InvalidOperationException;

class AssistantProposalStoreTest {

	private static final Long TENANT_ID = 10L;
	private static final Long OTHER_TENANT_ID = 99L;

	private AtomicReference<Instant> now;
	private AssistantProposalStore store;

	@BeforeEach
	void setUp() {
		now = new AtomicReference<>(Instant.parse("2026-09-14T12:00:00Z"));
		store = new AssistantProposalStore(
				new AssistantProperties(Duration.ofMinutes(10)),
				clock(now));
	}

	@Test
	void consumeReturnsAProposalOnlyForTheOwningTenant() {
		String proposalId = store.save(TENANT_ID, proposal());

		assertThrows(
				InvalidOperationException.class,
				() -> store.consume(proposalId, OTHER_TENANT_ID));
		assertEquals("Emma", store.consume(proposalId, TENANT_ID).firstName());
	}

	@Test
	void consumeIsOneTimeUse() {
		String proposalId = store.save(TENANT_ID, proposal());
		store.consume(proposalId, TENANT_ID);

		assertThrows(
				InvalidOperationException.class,
				() -> store.consume(proposalId, TENANT_ID));
	}

	@Test
	void consumeRejectsExpiredProposals() {
		String proposalId = store.save(TENANT_ID, proposal());
		now.set(Instant.parse("2026-09-14T12:10:00Z"));

		assertThrows(
				InvalidOperationException.class,
				() -> store.consume(proposalId, TENANT_ID));
	}

	@Test
	void restoreAllowsRetryAfterAFailedBooking() {
		String proposalId = store.save(TENANT_ID, proposal());
		AssistantProposalResponse consumed = store.consume(proposalId, TENANT_ID);
		store.restore(proposalId, TENANT_ID, consumed);

		assertEquals("Emma", store.consume(proposalId, TENANT_ID).firstName());
	}

	private AssistantProposalResponse proposal() {
		return new AssistantProposalResponse(
				null,
				true,
				40L,
				"Anna",
				"Smith",
				50L,
				"Haircut",
				new BigDecimal("30.00"),
				60,
				Instant.parse("2026-09-14T09:00:00Z"),
				Instant.parse("2026-09-14T10:00:00Z"),
				"Emma",
				"Chen",
				"emma@example.com",
				"555-0100",
				null);
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
