package com.bookflow.backend.ai;

@FunctionalInterface
interface LlmTransport {

	String postChatCompletions(String jsonBody);
}
