package com.bookflow.backend.ai;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.bookflow.backend.ai.dto.AssistantProposalResponse;
import com.bookflow.backend.common.exception.InvalidOperationException;

@Component
public class AssistantProposalStore {

	private final AssistantProperties properties;
	private final Clock clock;
	private final ConcurrentHashMap<String, StoredProposal> proposals = new ConcurrentHashMap<>();

	public AssistantProposalStore(AssistantProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
	}

	public String save(Long tenantId, AssistantProposalResponse proposal) {
		String proposalId = UUID.randomUUID().toString();
		proposals.put(
				proposalId,
				new StoredProposal(
						tenantId,
						proposal,
						Instant.now(clock).plus(properties.proposalTtl())));
		return proposalId;
	}

	public AssistantProposalResponse consume(String proposalId, Long tenantId) {
		if (proposalId == null || proposalId.isBlank()) {
			throw invalidProposal();
		}
		Instant now = Instant.now(clock);
		AtomicReference<AssistantProposalResponse> taken = new AtomicReference<>();
		proposals.computeIfPresent(proposalId, (id, stored) -> {
			if (!stored.expiresAt().isAfter(now)) {
				return null;
			}
			if (!stored.tenantId().equals(tenantId)) {
				return stored;
			}
			taken.set(stored.proposal());
			return null;
		});
		if (taken.get() == null) {
			throw invalidProposal();
		}
		return taken.get();
	}

	public void restore(String proposalId, Long tenantId, AssistantProposalResponse proposal) {
		proposals.putIfAbsent(
				proposalId,
				new StoredProposal(
						tenantId,
						proposal,
						Instant.now(clock).plus(properties.proposalTtl())));
	}

	public void clear() {
		proposals.clear();
	}

	private InvalidOperationException invalidProposal() {
		return new InvalidOperationException("This booking proposal is no longer valid");
	}

	private record StoredProposal(
			Long tenantId,
			AssistantProposalResponse proposal,
			Instant expiresAt) {
	}
}
