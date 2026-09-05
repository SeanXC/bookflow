package com.bookflow.backend.common.error;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.bookflow.backend.common.exception.ResourceNotFoundException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
			ResourceNotFoundException exception) {
		return buildResponse(
				HttpStatus.NOT_FOUND,
				"RESOURCE_NOT_FOUND",
				exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(error -> "%s: %s".formatted(error.getField(), error.getDefaultMessage()))
				.distinct()
				.collect(Collectors.joining(", "));

		return buildResponse(
				HttpStatus.BAD_REQUEST,
				"VALIDATION_ERROR",
				message.isBlank() ? "Request validation failed." : message);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
			ConstraintViolationException exception) {
		String message = exception.getConstraintViolations()
				.stream()
				.map(violation -> "%s: %s".formatted(
						violation.getPropertyPath(),
						violation.getMessage()))
				.distinct()
				.collect(Collectors.joining(", "));

		return buildResponse(
				HttpStatus.BAD_REQUEST,
				"VALIDATION_ERROR",
				message.isBlank() ? "Request validation failed." : message);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleUnreadableMessage() {
		return buildResponse(
				HttpStatus.BAD_REQUEST,
				"MALFORMED_REQUEST",
				"The request body is missing or malformed.");
	}

	private ResponseEntity<ApiErrorResponse> buildResponse(
			HttpStatus status,
			String error,
			String message) {
		ApiErrorResponse response = new ApiErrorResponse(
				status.value(),
				error,
				message,
				Instant.now());
		return ResponseEntity.status(status).body(response);
	}
}
