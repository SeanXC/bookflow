package com.bookflow.backend.ai;

import java.util.List;

public final class BookingToolContract {

	public static final String LIST_SERVICES = "list_services";
	public static final String LIST_STAFF = "list_staff";
	public static final String LIST_SLOTS = "list_slots";
	public static final String PROPOSE_BOOKING = "propose_booking";

	private BookingToolContract() {
	}

	public static List<LlmTool> tools() {
		return List.of(
				new LlmTool(
						LIST_SERVICES,
						"List the active services that guests can book.",
						"""
						{
						  "type": "object",
						  "properties": {},
						  "additionalProperties": false
						}
						"""),
				new LlmTool(
						LIST_STAFF,
						"List the active staff members who can be booked.",
						"""
						{
						  "type": "object",
						  "properties": {},
						  "additionalProperties": false
						}
						"""),
				new LlmTool(
						LIST_SLOTS,
						"List real bookable slots for one staff member and service.",
						"""
						{
						  "type": "object",
						  "properties": {
						    "staffId": { "type": "integer" },
						    "serviceId": { "type": "integer" },
						    "from": {
						      "type": "string",
						      "description": "Inclusive start date YYYY-MM-DD"
						    },
						    "to": {
						      "type": "string",
						      "description": "Inclusive end date YYYY-MM-DD"
						    }
						  },
						  "required": ["staffId", "serviceId", "from", "to"],
						  "additionalProperties": false
						}
						"""),
				new LlmTool(
						PROPOSE_BOOKING,
						"Propose a booking using a real slot. Do not treat this as a confirmed appointment.",
						"""
						{
						  "type": "object",
						  "properties": {
						    "staffId": { "type": "integer" },
						    "serviceId": { "type": "integer" },
						    "startTime": {
						      "type": "string",
						      "description": "ISO-8601 UTC instant returned by list_slots"
						    },
						    "firstName": { "type": "string" },
						    "lastName": { "type": "string" },
						    "email": { "type": "string" },
						    "phone": { "type": "string" },
						    "notes": { "type": "string" }
						  },
						  "required": [
						    "staffId",
						    "serviceId",
						    "startTime",
						    "firstName",
						    "lastName",
						    "email",
						    "phone"
						  ],
						  "additionalProperties": false
						}
						"""));
	}
}
