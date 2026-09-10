package com.bookflow.backend.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableConfigurationProperties({
	LlmProperties.class,
	AssistantProperties.class,
	AssistantRateLimitProperties.class
})
public class LlmConfiguration {

	@Bean
	RestClient llmRestClient(LlmProperties properties) {
		SimpleClientHttpRequestFactory requestFactory =
				new SimpleClientHttpRequestFactory();
		int timeoutMillis = Math.toIntExact(properties.timeout().toMillis());
		requestFactory.setConnectTimeout(timeoutMillis);
		requestFactory.setReadTimeout(timeoutMillis);
		return RestClient.builder()
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory)
				.build();
	}

	@Bean
	LlmClient llmClient(
			LlmProperties properties,
			JsonMapper jsonMapper,
			RestClient llmRestClient) {
		return new OpenAiCompatibleLlmClient(
				properties,
				jsonMapper,
				body -> llmRestClient.post()
						.uri("/chat/completions")
						.contentType(MediaType.APPLICATION_JSON)
						.headers(headers -> {
							if (properties.isConfigured()) {
								headers.set(
										HttpHeaders.AUTHORIZATION,
										"Bearer " + properties.apiKey());
							}
						})
						.body(body)
						.retrieve()
						.body(String.class));
	}
}
