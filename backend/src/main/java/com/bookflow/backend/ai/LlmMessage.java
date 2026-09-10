package com.bookflow.backend.ai;

import java.util.List;

public record LlmMessage(
		LlmRole role,
		String content,
		List<LlmToolCall> toolCalls,
		String toolCallId,
		String name) {

	public static LlmMessage system(String content) {
		return new LlmMessage(LlmRole.SYSTEM, content, List.of(), null, null);
	}

	public static LlmMessage user(String content) {
		return new LlmMessage(LlmRole.USER, content, List.of(), null, null);
	}

	public static LlmMessage assistant(String content, List<LlmToolCall> toolCalls) {
		return new LlmMessage(
				LlmRole.ASSISTANT,
				content,
				toolCalls == null ? List.of() : List.copyOf(toolCalls),
				null,
				null);
	}

	public static LlmMessage tool(String toolCallId, String name, String content) {
		return new LlmMessage(LlmRole.TOOL, content, List.of(), toolCallId, name);
	}
}
