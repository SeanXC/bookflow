package com.bookflow.backend.ai;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import tools.jackson.databind.json.JsonMapper;

@TestConfiguration(proxyBeanMethods = false)
public class AssistantMockLlmConfiguration {

	@Bean
	@Primary
	ScriptedBookingLlmClient scriptedBookingLlmClient(JsonMapper jsonMapper) {
		return new ScriptedBookingLlmClient(jsonMapper);
	}
}
