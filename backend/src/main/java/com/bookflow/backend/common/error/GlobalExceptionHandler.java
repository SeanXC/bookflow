package com.bookflow.backend.common.error;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.bookflow.backend.common.exception.AppointmentConflictException;
import com.bookflow.backend.common.exception.DuplicateResourceException;
import com.bookflow.backend.common.exception.InvalidOperationException;
import com.bookflow.backend.common.exception.ResourceNotFoundException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiErrorResponse> handleAccessDeniedException() {
		return buildResponse(
				HttpStatus.FORBIDDEN,
				"FORBIDDEN",
				"You do not have permission to perform this action.");
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

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiErrorResponse> handleArgumentTypeMismatch(
			MethodArgumentTypeMismatchException exception) {
		return buildResponse(
				HttpStatus.BAD_REQUEST,
				"INVALID_PARAMETER",
				"Invalid value for parameter '%s'.".formatted(exception.getName()));
	}

	@ExceptionHandler(PropertyReferenceException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidSortProperty(
			PropertyReferenceException exception) {
		return buildResponse(
				HttpStatus.BAD_REQUEST,
				"INVALID_PARAMETER",
				"Invalid sort property '%s'.".formatted(exception.getPropertyName()));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiErrorResponse> handleMissingParameter(
			MissingServletRequestParameterException exception) {
		return buildResponse(
				HttpStatus.BAD_REQUEST,
				"MISSING_PARAMETER",
				"Required parameter '%s' is missing.".formatted(
						exception.getParameterName()));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNoResourceFound() {
		return buildResponse(
				HttpStatus.NOT_FOUND,
				"RESOURCE_NOT_FOUND",
				"The requested endpoint does not exist.");
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
			HttpRequestMethodNotSupportedException exception) {
		String method = exception.getMethod();
		return buildResponse(
				HttpStatus.METHOD_NOT_ALLOWED,
				"METHOD_NOT_ALLOWED",
				"HTTP method '%s' is not supported for this endpoint."
						.formatted(method == null ? "UNKNOWN" : method));
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported() {
		return buildResponse(
				HttpStatus.UNSUPPORTED_MEDIA_TYPE,
				"UNSUPPORTED_MEDIA_TYPE",
				"The request content type is not supported.");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
			Exception exception) {
		log.error("Unhandled request exception", exception);
		return buildResponse(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"INTERNAL_ERROR",
				"An unexpected error occurred.");
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
