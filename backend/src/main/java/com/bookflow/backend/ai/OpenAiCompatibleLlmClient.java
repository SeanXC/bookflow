package com.bookflow.backend.ai;

import com.bookflow.backend.common.exception.InvalidOperationException;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
public class OpenAiCompatibleLlmClient implements LlmClient {

	private final LlmProperties properties;
	private final JsonMapper jsonMapper;
	private final LlmTransport transport;

	OpenAiCompatibleLlmClient(
			LlmProperties properties,
			JsonMapper jsonMapper,
			LlmTransport transport) {
		this.properties = properties;
		this.jsonMapper = jsonMapper;
		this.transport = transport;
	}

	@Override
	public LlmCompletion complete(LlmRequest request) {
		if (!properties.isConfigured()) {
			throw new InvalidOperationException(
					"The booking assistant is not configured.");
		}
		try {
			String responseBody = transport.postChatCompletions(
					OpenAiChatCodec.writeRequest(
							jsonMapper,
							properties.model(),
							request));
			return OpenAiChatCodec.readCompletion(jsonMapper, responseBody);
		} catch (InvalidOperationException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			log.warn("LLM completion failed", exception);
			throw new InvalidOperationException(
					"The booking assistant is temporarily unavailable.");
		}
	}
}
