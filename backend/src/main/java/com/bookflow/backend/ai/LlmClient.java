package com.bookflow.backend.ai;

public interface LlmClient {

	LlmCompletion complete(LlmRequest request);
}
