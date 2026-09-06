package com.bookflow.backend.common.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Stable pagination envelope")
public record PageResponse<T>(
		List<T> content,
		@Schema(example = "0")
		int page,
		@Schema(example = "20")
		int size,
		@Schema(example = "42")
		long totalElements,
		@Schema(example = "3")
		int totalPages) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages());
	}
}
