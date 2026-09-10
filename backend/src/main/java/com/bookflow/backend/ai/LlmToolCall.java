package com.bookflow.backend.ai;

public record LlmToolCall(
		String id,
		String name,
		String argumentsJson) {
}
