package com.bookflow.backend.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class BookingToolContractTest {

	@Test
	void toolsExposeTheBookingContractWithValidJsonSchemas() {
		JsonMapper jsonMapper = JsonMapper.builder().build();
		var tools = BookingToolContract.tools();

		assertEquals(
				Set.of(
						BookingToolContract.LIST_SERVICES,
						BookingToolContract.LIST_STAFF,
						BookingToolContract.LIST_SLOTS,
						BookingToolContract.PROPOSE_BOOKING),
				tools.stream().map(LlmTool::name).collect(Collectors.toSet()));

		for (LlmTool tool : tools) {
			assertTrue(jsonMapper.readTree(tool.parametersJson()).isObject());
		}

		String slotSchema = tools.stream()
				.filter(tool -> tool.name().equals(BookingToolContract.LIST_SLOTS))
				.findFirst()
				.orElseThrow()
				.parametersJson();
		assertTrue(slotSchema.contains("staffId"));
		assertTrue(slotSchema.contains("serviceId"));
	}
}
