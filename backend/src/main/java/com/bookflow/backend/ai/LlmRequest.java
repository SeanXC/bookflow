package com.bookflow.backend.ai;

import java.util.List;

public record LlmRequest(
		List<LlmMessage> messages,
		List<LlmTool> tools) {

	public LlmRequest {
		messages = List.copyOf(messages);
		tools = tools == null ? List.of() : List.copyOf(tools);
	}
}
