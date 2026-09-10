package com.bookflow.backend.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.bookflow.backend.common.exception.InvalidOperationException;

import tools.jackson.databind.json.JsonMapper;

class OpenAiCompatibleLlmClientTest {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@Test
	void completePostsThePromptContractThroughTheTransport() {
		AtomicReference<String> posted = new AtomicReference<>();
		LlmClient client = new OpenAiCompatibleLlmClient(
				properties("test-key"),
				jsonMapper,
				body -> {
					posted.set(body);
					return """
							{
							  "choices": [
							    {
							      "finish_reason": "stop",
							      "message": {
							        "role": "assistant",
							        "content": "I can help you book a haircut."
							      }
							    }
							  ]
							}
							""";
				});

		LlmCompletion completion = client.complete(new LlmRequest(
				List.of(LlmMessage.user("I need a haircut")),
				BookingToolContract.tools()));

		assertEquals("I can help you book a haircut.", completion.content());
		assertTrue(posted.get().contains("I need a haircut"));
		assertTrue(posted.get().contains(BookingToolContract.LIST_SLOTS));
	}

	@Test
	void completeRejectsAMissingApiKeyWithoutCallingTheTransport() {
		AtomicReference<Boolean> called = new AtomicReference<>(false);
		LlmClient client = new OpenAiCompatibleLlmClient(
				properties(""),
				jsonMapper,
				body -> {
					called.set(true);
					return "{}";
				});

		assertThrows(
				InvalidOperationException.class,
				() -> client.complete(new LlmRequest(
						List.of(LlmMessage.user("hi")),
						List.of())));
		assertEquals(false, called.get());
	}

	@Test
	void completeHidesTransportFailures() {
		LlmClient client = new OpenAiCompatibleLlmClient(
				properties("test-key"),
				jsonMapper,
				body -> {
					throw new IllegalStateException("connection reset");
				});

		InvalidOperationException exception = assertThrows(
				InvalidOperationException.class,
				() -> client.complete(new LlmRequest(
						List.of(LlmMessage.user("hi")),
						List.of())));
		assertEquals(
				"The booking assistant is temporarily unavailable.",
				exception.getMessage());
	}

	private LlmProperties properties(String apiKey) {
		return new LlmProperties(
				"https://api.openai.com/v1",
				apiKey,
				"gpt-4o-mini",
				Duration.ofSeconds(20));
	}
}
