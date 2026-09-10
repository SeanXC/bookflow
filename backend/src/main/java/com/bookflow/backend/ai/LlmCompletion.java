package com.bookflow.backend.ai;

import java.util.List;

public record LlmCompletion(
		String content,
		List<LlmToolCall> toolCalls,
		String finishReason) {

	public LlmCompletion {
		toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
	}

	public boolean hasToolCalls() {
		return !toolCalls.isEmpty();
	}
}
