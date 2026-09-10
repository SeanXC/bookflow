package com.bookflow.backend.ai;

import java.io.IOException;
import java.time.Instant;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import com.bookflow.backend.common.error.ApiErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 21)
@RequiredArgsConstructor
public class AssistantRateLimitFilter extends OncePerRequestFilter {

	private static final PathPattern PUBLIC_CONFIRM_PATH = PathPatternParser.defaultInstance
			.parse("/api/public/businesses/{slug}/assistant/confirm");
	private static final PathPattern PUBLIC_CHAT_PATH = PathPatternParser.defaultInstance
			.parse("/api/public/businesses/{slug}/assistant");
	private static final PathPattern TENANT_CONFIRM_PATH = PathPatternParser.defaultInstance
			.parse("/api/assistant/confirm");
	private static final PathPattern TENANT_CHAT_PATH = PathPatternParser.defaultInstance
			.parse("/api/assistant");

	private final AssistantRateLimiter rateLimiter;
	private final JsonMapper jsonMapper;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		if (!HttpMethod.POST.matches(request.getMethod())) {
			filterChain.doFilter(request, response);
			return;
		}

		String scope = resolveScope(request.getRequestURI());
		if (scope == null) {
			filterChain.doFilter(request, response);
			return;
		}

		if (rateLimiter.tryConsume(clientIp(request), scope)) {
			filterChain.doFilter(request, response);
			return;
		}

		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setHeader(
				"Retry-After",
				String.valueOf(rateLimiter.window().toSeconds()));
		jsonMapper.writeValue(
				response.getOutputStream(),
				new ApiErrorResponse(
						HttpStatus.TOO_MANY_REQUESTS.value(),
						"RATE_LIMITED",
						"Too many assistant requests. Please try again later.",
						Instant.now()));
	}

	private String resolveScope(String requestUri) {
		PathContainer path = PathContainer.parsePath(requestUri);
		PathPattern.PathMatchInfo publicConfirm = PUBLIC_CONFIRM_PATH.matchAndExtract(path);
		if (publicConfirm != null) {
			return publicConfirm.getUriVariables().get("slug");
		}
		PathPattern.PathMatchInfo publicChat = PUBLIC_CHAT_PATH.matchAndExtract(path);
		if (publicChat != null) {
			return publicChat.getUriVariables().get("slug");
		}
		if (TENANT_CONFIRM_PATH.matches(path) || TENANT_CHAT_PATH.matches(path)) {
			return "authenticated";
		}
		return null;
	}

	private String clientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		String remoteAddr = request.getRemoteAddr();
		return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr;
	}
}
