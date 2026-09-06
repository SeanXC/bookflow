package com.bookflow.backend.common.error;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.bookflow.backend.common.exception.AppointmentConflictException;
import com.bookflow.backend.common.exception.DuplicateResourceException;
import com.bookflow.backend.common.exception.InvalidOperationException;
import com.bookflow.backend.common.exception.ResourceNotFoundException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AppointmentConflictException.class)
	public ResponseEntity<ApiErrorResponse> handleAppointmentConflict(
			AppointmentConflictException exception) {
		return buildResponse(
				HttpStatus.CONFLICT,
				"BOOKING_CONFLICT",
				exception.getMessage());
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
			ResourceNotFoundException exception) {
		return buildResponse(
				HttpStatus.NOT_FOUND,
				"RESOURCE_NOT_FOUND",
				exception.getMessage());
	}

	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ApiErrorResponse> handleDuplicateResource(
			DuplicateResourceException exception) {
		return buildResponse(
				HttpStatus.CONFLICT,
				"DUPLICATE_RESOURCE",
				exception.getMessage());
	}

	@ExceptionHandler(InvalidOperationException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidOperation(
			InvalidOperationException exception) {
		return buildResponse(
				HttpStatus.BAD_REQUEST,
				"INVALID_OPERATION",
				exception.getMessage());
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiErrorResponse> handleAuthenticationException() {
		return buildResponse(
				HttpStatus.UNAUTHORIZED,
				"INVALID_CREDENTIALS",
				"Invalid email or password.");
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
