package com.bookflow.backend.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class OpenAiChatCodecTest {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@Test
	void writeRequestUsesTheOpenAiChatCompletionsShape() {
		String body = OpenAiChatCodec.writeRequest(
				jsonMapper,
				"gpt-4o-mini",
				new LlmRequest(
						List.of(
								LlmMessage.system("Stay in tenant"),
								LlmMessage.user("Book a haircut")),
						BookingToolContract.tools()));

		JsonNode root = jsonMapper.readTree(body);
		assertEquals("gpt-4o-mini", root.path("model").asString());
		assertEquals("system", root.path("messages").path(0).path("role").asString());
		assertEquals("function", root.path("tools").path(0).path("type").asString());
		assertEquals(
				BookingToolContract.LIST_SERVICES,
				root.path("tools").path(0).path("function").path("name").asString());
		assertTrue(root.path("tools").path(0).path("function").path("parameters").isObject());
	}

	@Test
	void readCompletionParsesAssistantToolCalls() {
		LlmCompletion completion = OpenAiChatCodec.readCompletion(
				jsonMapper,
				"""
				{
				  "choices": [
				    {
				      "finish_reason": "tool_calls",
				      "message": {
				        "role": "assistant",
				        "content": null,
				        "tool_calls": [
				          {
				            "id": "call_1",
				            "type": "function",
				            "function": {
				              "name": "list_slots",
				              "arguments": "{\\"staffId\\":40}"
				            }
				          }
				        ]
				      }
				    }
				  ]
				}
				""");

		assertTrue(completion.hasToolCalls());
		assertEquals("tool_calls", completion.finishReason());
		assertEquals("call_1", completion.toolCalls().getFirst().id());
		assertEquals("list_slots", completion.toolCalls().getFirst().name());
		assertEquals("{\"staffId\":40}", completion.toolCalls().getFirst().argumentsJson());
	}
}
