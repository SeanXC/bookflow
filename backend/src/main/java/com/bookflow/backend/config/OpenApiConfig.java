package com.bookflow.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	private static final String BEARER_AUTH = "bearerAuth";

	@Bean
	OpenAPI bookFlowOpenApi() {
		SecurityScheme bearerScheme = new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT");

		return new OpenAPI()
				.info(new Info()
						.title("BookFlow API")
						.description("""
								Multi-tenant appointment management API for small service businesses.
								Tenant context is derived from the authenticated user and is never
								accepted from client requests.
								""")
						.version("v1"))
				.components(new Components()
						.addSecuritySchemes(BEARER_AUTH, bearerScheme))
				.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
	}
}
