package com.bookflow.backend.common.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.bookflow.backend.common.exception.AppointmentConflictException;
import com.bookflow.backend.common.exception.DuplicateResourceException;
import com.bookflow.backend.common.exception.InvalidOperationException;
import com.bookflow.backend.common.exception.ResourceNotFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

class GlobalExceptionHandlerTest {

	private GlobalExceptionHandler handler;

	@BeforeEach
	void setUp() {
		handler = new GlobalExceptionHandler();
	}

	@Test
	void mapsDomainExceptionsToTheStableErrorContract() {
		assertResponse(
				handler.handleAppointmentConflict(new AppointmentConflictException()),
				HttpStatus.CONFLICT,
				"BOOKING_CONFLICT",
				"This staff member already has an appointment during the selected time.");
		assertResponse(
				handler.handleResourceNotFound(new ResourceNotFoundException("Staff", 42L)),
				HttpStatus.NOT_FOUND,
				"RESOURCE_NOT_FOUND",
				"Staff not found: 42");
		assertResponse(
				handler.handleDuplicateResource(new DuplicateResourceException("Duplicate")),
				HttpStatus.CONFLICT,
				"DUPLICATE_RESOURCE",
				"Duplicate");
		assertResponse(
				handler.handleInvalidOperation(new InvalidOperationException("Invalid")),
				HttpStatus.BAD_REQUEST,
				"INVALID_OPERATION",
				"Invalid");
	}

	@Test
	void mapsAuthenticationAndAuthorizationFailures() {
		assertResponse(
				handler.handleAuthenticationException(),
				HttpStatus.UNAUTHORIZED,
				"INVALID_CREDENTIALS",
				"Invalid email or password.");
		assertResponse(
				handler.handleAccessDeniedException(),
				HttpStatus.FORBIDDEN,
				"FORBIDDEN",
				"You do not have permission to perform this action.");
	}

	@Test
	void mapsFieldValidationErrorsAndRemovesDuplicates() {
		MethodArgumentNotValidException exception =
				mock(MethodArgumentNotValidException.class);
		BindingResult bindingResult = mock(BindingResult.class);
		when(exception.getBindingResult()).thenReturn(bindingResult);
		when(bindingResult.getFieldErrors()).thenReturn(List.of(
				new FieldError("request", "email", "must be valid"),
				new FieldError("request", "email", "must be valid")));

		assertResponse(
				handler.handleMethodArgumentNotValid(exception),
				HttpStatus.BAD_REQUEST,
				"VALIDATION_ERROR",
				"email: must be valid");
	}

	@Test
	void fieldValidationUsesAFallbackWhenNoMessageExists() {
		MethodArgumentNotValidException exception =
				mock(MethodArgumentNotValidException.class);
		BindingResult bindingResult = mock(BindingResult.class);
		when(exception.getBindingResult()).thenReturn(bindingResult);
		when(bindingResult.getFieldErrors()).thenReturn(List.of());

		assertResponse(
				handler.handleMethodArgumentNotValid(exception),
				HttpStatus.BAD_REQUEST,
				"VALIDATION_ERROR",
				"Request validation failed.");
	}

	@Test
	void mapsConstraintViolationsAndTheirEmptyFallback() {
		@SuppressWarnings("unchecked")
		ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
		Path path = mock(Path.class);
		when(path.toString()).thenReturn("email");
		when(violation.getPropertyPath()).thenReturn(path);
		when(violation.getMessage()).thenReturn("must be valid");

		assertResponse(
				handler.handleConstraintViolation(
						new ConstraintViolationException(Set.of(violation))),
				HttpStatus.BAD_REQUEST,
				"VALIDATION_ERROR",
				"email: must be valid");
		assertResponse(
				handler.handleConstraintViolation(
						new ConstraintViolationException(Set.of())),
				HttpStatus.BAD_REQUEST,
				"VALIDATION_ERROR",
				"Request validation failed.");
	}

	@Test
	void mapsInvalidRequestProtocolErrors() {
		assertResponse(
				handler.handleUnreadableMessage(),
				HttpStatus.BAD_REQUEST,
				"MALFORMED_REQUEST",
				"The request body is missing or malformed.");

		MethodArgumentTypeMismatchException mismatch =
				mock(MethodArgumentTypeMismatchException.class);
		when(mismatch.getName()).thenReturn("status");
		assertResponse(
				handler.handleArgumentTypeMismatch(mismatch),
				HttpStatus.BAD_REQUEST,
				"INVALID_PARAMETER",
				"Invalid value for parameter 'status'.");

		PropertyReferenceException property = mock(PropertyReferenceException.class);
		when(property.getPropertyName()).thenReturn("unknown");
		assertResponse(
				handler.handleInvalidSortProperty(property),
				HttpStatus.BAD_REQUEST,
				"INVALID_PARAMETER",
				"Invalid sort property 'unknown'.");

		assertResponse(
				handler.handleMissingParameter(
						new MissingServletRequestParameterException("from", "date")),
				HttpStatus.BAD_REQUEST,
				"MISSING_PARAMETER",
				"Required parameter 'from' is missing.");
	}

	@Test
	void mapsRoutingAndMediaTypeErrors() {
		assertResponse(
				handler.handleNoResourceFound(),
				HttpStatus.NOT_FOUND,
				"RESOURCE_NOT_FOUND",
				"The requested endpoint does not exist.");
		assertResponse(
				handler.handleMethodNotSupported(
						new HttpRequestMethodNotSupportedException("TRACE")),
				HttpStatus.METHOD_NOT_ALLOWED,
				"METHOD_NOT_ALLOWED",
				"HTTP method 'TRACE' is not supported for this endpoint.");

		HttpRequestMethodNotSupportedException unknownMethod =
				mock(HttpRequestMethodNotSupportedException.class);
		when(unknownMethod.getMethod()).thenReturn(null);
		assertResponse(
				handler.handleMethodNotSupported(unknownMethod),
				HttpStatus.METHOD_NOT_ALLOWED,
				"METHOD_NOT_ALLOWED",
				"HTTP method 'UNKNOWN' is not supported for this endpoint.");
		assertResponse(
				handler.handleMediaTypeNotSupported(),
				HttpStatus.UNSUPPORTED_MEDIA_TYPE,
				"UNSUPPORTED_MEDIA_TYPE",
				"The request content type is not supported.");
	}

	@Test
	void hidesUnexpectedExceptionDetails() {
		assertResponse(
				handler.handleUnexpectedException(new IllegalStateException("secret detail")),
				HttpStatus.INTERNAL_SERVER_ERROR,
				"INTERNAL_ERROR",
				"An unexpected error occurred.");
	}

	private void assertResponse(
			ResponseEntity<ApiErrorResponse> response,
			HttpStatus expectedStatus,
			String expectedError,
			String expectedMessage) {
		assertEquals(expectedStatus, response.getStatusCode());
		ApiErrorResponse body = response.getBody();
		assertNotNull(body);
		assertEquals(expectedStatus.value(), body.status());
		assertEquals(expectedError, body.error());
		assertEquals(expectedMessage, body.message());
		assertNotNull(body.timestamp());
	}
}
