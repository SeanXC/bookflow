package com.bookflow.backend.ai;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

final class OpenAiChatCodec {

	private OpenAiChatCodec() {
	}

	static String writeRequest(
			JsonMapper jsonMapper,
			String model,
			LlmRequest request) {
		ObjectNode root = jsonMapper.createObjectNode();
		root.put("model", model);
		ArrayNode messages = root.putArray("messages");
		for (LlmMessage message : request.messages()) {
			messages.add(writeMessage(jsonMapper, message));
		}
		if (!request.tools().isEmpty()) {
			ArrayNode tools = root.putArray("tools");
			for (LlmTool tool : request.tools()) {
				ObjectNode toolNode = tools.addObject();
				toolNode.put("type", "function");
				ObjectNode function = toolNode.putObject("function");
				function.put("name", tool.name());
				function.put("description", tool.description());
				function.set("parameters", jsonMapper.readTree(tool.parametersJson()));
			}
		}
		return jsonMapper.writeValueAsString(root);
	}

	static LlmCompletion readCompletion(JsonMapper jsonMapper, String responseBody) {
		JsonNode root = jsonMapper.readTree(responseBody);
		JsonNode message = root.path("choices").path(0).path("message");
		if (message.isMissingNode() || message.isNull()) {
			throw new IllegalStateException("The LLM response did not include a message");
		}
		return new LlmCompletion(
				textOrNull(message.get("content")),
				readToolCalls(message.path("tool_calls")),
				textOrNull(root.path("choices").path(0).get("finish_reason")));
	}

	private static ObjectNode writeMessage(JsonMapper jsonMapper, LlmMessage message) {
		ObjectNode node = jsonMapper.createObjectNode();
		node.put("role", message.role().name().toLowerCase());
		if (message.content() != null) {
			node.put("content", message.content());
		}
		if (message.toolCallId() != null) {
			node.put("tool_call_id", message.toolCallId());
		}
		if (message.name() != null && message.role() == LlmRole.TOOL) {
			node.put("name", message.name());
		}
		if (!message.toolCalls().isEmpty()) {
			ArrayNode toolCalls = node.putArray("tool_calls");
			for (LlmToolCall toolCall : message.toolCalls()) {
				ObjectNode call = toolCalls.addObject();
				call.put("id", toolCall.id());
				call.put("type", "function");
				ObjectNode function = call.putObject("function");
				function.put("name", toolCall.name());
				function.put("arguments", toolCall.argumentsJson());
			}
		}
		return node;
	}

	private static List<LlmToolCall> readToolCalls(JsonNode toolCalls) {
		if (!toolCalls.isArray() || toolCalls.isEmpty()) {
			return List.of();
		}
		List<LlmToolCall> calls = new ArrayList<>();
		for (JsonNode toolCall : toolCalls) {
			JsonNode function = toolCall.path("function");
			calls.add(new LlmToolCall(
					textOrNull(toolCall.get("id")),
					textOrNull(function.get("name")),
					textOrEmpty(function.get("arguments"))));
		}
		return List.copyOf(calls);
	}

	private static String textOrNull(JsonNode node) {
		if (node == null || node.isNull() || node.isMissingNode()) {
			return null;
		}
		String value = node.asString();
		return value.isBlank() ? null : value;
	}

	private static String textOrEmpty(JsonNode node) {
		if (node == null || node.isNull() || node.isMissingNode()) {
			return "{}";
		}
		String value = node.asString();
		return value.isBlank() ? "{}" : value;
	}
}
