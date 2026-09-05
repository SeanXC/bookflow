package com.bookflow.backend.common.exception;

public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String resourceName, Object identifier) {
		super("%s not found: %s".formatted(resourceName, identifier));
	}
}
