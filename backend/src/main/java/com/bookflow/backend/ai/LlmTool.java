package com.bookflow.backend.ai;

public record LlmTool(
		String name,
		String description,
		String parametersJson) {
}
